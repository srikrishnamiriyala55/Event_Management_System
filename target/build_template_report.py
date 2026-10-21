from pathlib import Path
from copy import deepcopy
import hashlib
import json
import re
from zipfile import ZipFile

from docx import Document
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_BREAK, WD_TAB_ALIGNMENT, WD_TAB_LEADER
from docx.shared import Inches, Pt, RGBColor
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.text.paragraph import Paragraph

ROOT = Path(__file__).resolve().parents[1]
REF = Path(r'C:\Users\srikr\Downloads\FSD-II FRONT PAGES TEMPLATE.docx')
IMAGE = Path(r'C:\Users\srikr\Downloads\Campus Event Management Architecture Infographic.png')
OUT = ROOT / 'docs' / 'Campus Event Management System Project Report.docx'
TITLE = 'Campus Event Management System'
COLLEGE = 'Lakireddy Bali Reddy College of Engineering (Autonomous)'
doc = Document(REF)
original = list(doc.paragraphs)
original_tables = list(doc.tables)
body = doc._element.body
for element in list(body):
    if element.tag != qn('w:sectPr'):
        body.remove(element)

def font(run, size=12, bold=None):
    run.font.name = 'Times New Roman'
    run.font.size = Pt(size)
    run.font.color.rgb = RGBColor(0, 0, 0)
    if bold is not None:
        run.bold = bold
    run.font.underline = False
    pr = run._r.get_or_add_rPr()
    rf = pr.find(qn('w:rFonts'))
    if rf is None:
        rf = OxmlElement('w:rFonts'); pr.insert(0, rf)
    for a in ('ascii', 'hAnsi', 'eastAsia', 'cs'):
        rf.set(qn('w:' + a), 'Times New Roman')
    for a in ('asciiTheme', 'hAnsiTheme', 'eastAsiaTheme', 'cstheme'):
        rf.attrib.pop(qn('w:' + a), None)

for st in doc.styles:
    if st.type in (1, 2):
        st.font.name = 'Times New Roman'
        st.font.size = Pt(12)
        st.font.color.rgb = RGBColor(0, 0, 0)
        pr = st.element.find(qn('w:pPr'))
        if pr is not None:
            for tag in ('pBdr', 'numPr'):
                n = pr.find(qn('w:' + tag))
                if n is not None: pr.remove(n)
for name, size in [('Title',16),('Heading 1',16),('Heading 2',14),('Heading 3',14)]:
    if name not in doc.styles:
        doc.styles.add_style(name, 1)
    doc.styles[name].font.size = Pt(size)

sec = doc.sections[0]
sec.top_margin = Inches(.78)
sec.bottom_margin = Inches(.83)
sec.left_margin = Inches(.85)
sec.right_margin = Inches(.75)
sec.header_distance = Inches(.1)
sec.footer_distance = Inches(.43)
sec.different_first_page_header_footer = False
for tag in ('titlePg',):
    element = sec._sectPr.find(qn('w:' + tag))
    if element is not None: sec._sectPr.remove(element)

def chrome(part, is_footer=False):
    for p in list(part.paragraphs)[1:]: p._element.getparent().remove(p._element)
    p = part.paragraphs[0]; p.clear()
    p.paragraph_format.space_before = Pt(0)
    p.paragraph_format.space_after = Pt(0)
    p.paragraph_format.line_spacing = 1
    if is_footer:
        p.alignment = WD_ALIGN_PARAGRAPH.LEFT
        p.paragraph_format.tab_stops.add_tab_stop(Inches(6.66), WD_TAB_ALIGNMENT.RIGHT)
        font(p.add_run(COLLEGE + '\t'))
        field = OxmlElement('w:fldSimple'); field.set(qn('w:instr'), 'PAGE')
        r=OxmlElement('w:r'); t=OxmlElement('w:t'); t.text='1'; r.append(t); field.append(r); p._p.append(field)
    else:
        p.alignment = WD_ALIGN_PARAGRAPH.RIGHT
        font(p.add_run(TITLE))
chrome(sec.header)
chrome(sec.footer, True)

def clean_p(p, size=12, align=WD_ALIGN_PARAGRAPH.JUSTIFY, indent=True, bold=False):
    p.style = doc.styles['Normal']
    p.alignment = align
    pf=p.paragraph_format
    pf.space_before=Pt(0); pf.space_after=Pt(7)
    pf.line_spacing=1.15
    pf.left_indent=Pt(0); pf.right_indent=Pt(0)
    pf.first_line_indent=Inches(.5) if indent else Pt(0)
    pf.page_break_before=False
    pf.keep_with_next=False
    pf.widow_control=True
    ppr=p._p.get_or_add_pPr()
    for tag in ('numPr','pBdr','tabs'):
        n=ppr.find(qn('w:'+tag))
        if n is not None: ppr.remove(n)
    for r in p.runs: font(r,size,bold)
    return p

def page():
    p=doc.add_paragraph(); p.paragraph_format.space_after=Pt(0)
    p.paragraph_format.line_spacing=1
    r=p.add_run(); font(r); r.add_break(WD_BREAK.PAGE)

bookmarks={}
def mark(p, name):
    idx=len(bookmarks)+1
    start=OxmlElement('w:bookmarkStart'); start.set(qn('w:id'),str(idx)); start.set(qn('w:name'),name)
    end=OxmlElement('w:bookmarkEnd'); end.set(qn('w:id'),str(idx))
    p._p.insert(0,start); p._p.append(end)
    bookmarks[name]=p.text

def heading(text, name=None, level=1, new=False):
    p=doc.add_paragraph(text,style='Heading '+str(level))
    clean_p(p,16 if level==1 else 14,WD_ALIGN_PARAGRAPH.CENTER if level==1 else WD_ALIGN_PARAGRAPH.LEFT,False,True)
    p.style=doc.styles['Heading '+str(level)]
    p.paragraph_format.space_before=Pt(5 if level==1 else 10)
    p.paragraph_format.space_after=Pt(12 if level==1 else 7)
    p.paragraph_format.keep_with_next=True
    p.paragraph_format.page_break_before=new
    if name: mark(p,name)
    return p

def para(text):
    return clean_p(doc.add_paragraph(text))

def copied(p):
    element=deepcopy(p._p)
    for b in list(element.xpath('.//w:br')): b.getparent().remove(b)
    for b in list(element.xpath('.//w:lastRenderedPageBreak')): b.getparent().remove(b)
    body.insert(len(body)-1,element)
    return Paragraph(element,doc._body)

def front(start,end,center=False):
    for i in range(start,end):
        source=original[i]
        if not source.text.strip() and not source._p.xpath('.//w:drawing'): continue
        p=copied(source)
        if p._p.xpath('.//w:drawing'):
            clean_p(p,align=WD_ALIGN_PARAGRAPH.CENTER,indent=False)
            for extent in p._p.xpath('.//wp:extent'):
                cx,cy=int(extent.get('cx')),int(extent.get('cy'))
                scale=min(1,1.1*914400/cy)
                extent.set('cx',str(int(cx*scale))); extent.set('cy',str(int(cy*scale)))
            for extent in p._p.xpath('.//a:xfrm/a:ext'):
                cx,cy=int(extent.get('cx')),int(extent.get('cy'))
                scale=min(1,1.1*914400/cy)
                extent.set('cx',str(int(cx*scale))); extent.set('cy',str(int(cy*scale)))
            continue
        text=p.text.replace('�','"')
        text=text.replace('<<SOC Web Application or Project Title>>',TITLE)
        text=text.replace('<<Web Application Title/Project Title>>',TITLE)
        if i==37: text='This is to certify that the Skill Oriented Course Web Application / Project entitled "'+TITLE+'" is being submitted by'
        if i==38: p._p.getparent().remove(p._p); continue
        if i==58: text='I am thankful to the teaching and non-teaching staff of the CSE department for their direct as well as indirect help in my Skill Oriented Course Web Application / Project.'
        if i==59: p._p.getparent().remove(p._p); continue
        p.text=text.strip()
        is_title=(i in [0,36,48,67])
        is_center=center or is_title or i in [26,27,28,29,30,32,33,39,65,73,75]
        clean_p(p,16 if is_title else 12,WD_ALIGN_PARAGRAPH.CENTER if is_center else WD_ALIGN_PARAGRAPH.JUSTIFY,not is_center,is_title)
        p.paragraph_format.space_after=Pt(5 if center else 12)
        p.paragraph_format.line_spacing=1 if center else 1.15
        if is_title: p.paragraph_format.space_after=Pt(16)

# Retain the original seven front-page patterns and college drawings.
front(0,26,True)
page(); front(26,48)
sig=doc.add_paragraph('INTERNAL EXAMINER\tEXTERNAL EXAMINER')
clean_p(sig,indent=False,bold=True)
sig.paragraph_format.space_before=Pt(24)
sig.paragraph_format.tab_stops.add_tab_stop(Inches(6.5),WD_TAB_ALIGNMENT.RIGHT)
page(); front(48,67)
page(); front(67,90)
page(); heading('ABSTRACT')
for text in [
    'Campus Event Management System is a full stack Java web application that centralizes campus event publication, participant registration, organizer administration, and registration reporting. It replaces disconnected notices and manual lists with a database-backed portal. The application provides two roles, Participant and Organizer, with separate dashboards and permissions.',
    'Participants register using a unique roll number, email address, and phone number. Email ownership is verified using a time-limited one-time password before account submission. Passwords are stored using BCrypt with a cost factor of 12. Participants browse event cards, filter by category and department, inspect event descriptions, and register for eligible events. Each completed registration receives a unique pass ID and a confirmation email. Registered events and participant profile information are available in separate sidebar sections.',
    'Organizers authenticate with their organizer ID and password followed by an email OTP. They create, update, view, and delete events, upload event images, define registration deadlines and capacities, and inspect registration counts. Searchable reports support event, college, department, and study-year filters. The application uses HTML, CSS, JavaScript, JSP, Servlets, JDBC, MySQL, and Apache Tomcat 9. HttpSession maintains authenticated state between login and logout. Payment is simulated in the current academic implementation and is recorded as PAID; no live payment gateway is integrated.'
]: para(text)

page(); heading('LIST OF CONTENTS')
toc_rows=[
('INTRODUCTION','intro',0),('Overview of the Project','overview',1),('Feasibility Study','feasibility',1),
('TECHNOLOGIES USED','technologies',0),('HTML, CSS & JavaScript (or) React JS','frontend',1),
('JDBC (Oracle or Mongo DB)','jdbc',1),('Servlets (or) Node JS & Express JS','servlets',1),('JSP','jsp',1),
('WEB APPLICATION ARCHITECTURE','architecture',0),('CODING & IMPLEMENTATION','coding',0),
('RESULTS','results',0),('CONCLUSION','conclusion',0),('REFERENCES','references',0)]
p=doc.add_paragraph('CONTENTS\tPAGE NO');clean_p(p,indent=False,bold=True)
p.paragraph_format.tab_stops.add_tab_stop(Inches(6.6),WD_TAB_ALIGNMENT.RIGHT)
for label,key,nested in toc_rows:
    p=doc.add_paragraph(label+'\t');clean_p(p,indent=False,bold=not nested)
    p.paragraph_format.space_after=Pt(10)
    p.paragraph_format.left_indent=Inches(.25) if nested else Pt(0)
    p.paragraph_format.tab_stops.add_tab_stop(Inches(6.6),WD_TAB_ALIGNMENT.RIGHT,WD_TAB_LEADER.DOTS)
    field=OxmlElement('w:fldSimple');field.set(qn('w:instr'),'PAGEREF '+key+' \\h')
    r=OxmlElement('w:r');t=OxmlElement('w:t');t.text='1';r.append(t);field.append(r);p._p.append(field)

page();heading('LIST OF ABBREVIATIONS')
for term,meaning in [('HTML','Hypertext Markup Language'),('CSS','Cascading Style Sheets'),('JS','JavaScript'),('JDBC','Java Database Connectivity'),('JSP','JavaServer Pages'),('JDK','Java Development Kit'),('JVM','Java Virtual Machine'),('SQL','Structured Query Language'),('DBMS','Database Management System'),('HTTP','Hypertext Transfer Protocol'),('SMTP','Simple Mail Transfer Protocol'),('OTP','One Time Password'),('CRUD','Create Read Update Delete'),('DAO','Data Access Object'),('MVC','Model View Controller'),('WAR','Web Application Archive'),('TLS','Transport Layer Security'),('BLOB','Binary Large Object')]:
    p=para(term+' - '+meaning);p.paragraph_format.first_line_indent=Pt(0)

heading('INTRODUCTION','intro',new=True)
heading('Overview of the Project','overview',2)
overview=[
    'Campus activities include technical competitions, workshops, sports tournaments, and cultural programmes. When announcements, registration forms, and participant lists are maintained separately, students may miss deadlines and organizers may have inconsistent records. The Campus Event Management System addresses this coordination problem through a single web application backed by a relational database.',
    'The main objective is to provide verified participant onboarding, convenient event discovery, controlled event registration, and accurate organizer reports. The system identifies students through their roll numbers and prevents duplicate email addresses and phone numbers. Organizer accounts are provisioned by initialization rather than through public registration.',
    'The application has two authenticated roles. Participants can browse available events, inspect details, register, retrieve a unique pass, and view their profile. Organizers maintain events, registration limits, deadlines, images, and availability, and inspect participant reports. These role boundaries are enforced in Servlet operations in addition to the authenticated route filter.',
    'The participant registration form includes text inputs, password and confirmation inputs, an address text area, college and department dropdowns, a four-year dropdown, gender radio buttons, profile image upload, a show-password checkbox, Submit, and Reset. An email OTP must be verified before registration is submitted; the server also checks the verified email state so client-side controls are not the only protection.',
    'Each participant dashboard contains Dashboard, Available Events, Registered Events, and Profile sections. Summary cards link to event lists. Event cards contain an image, category, department, date, venue, seats, fee, and action buttons. Descriptions are displayed only on the event details page. Both event lists support category and department filtering.',
    'Register now creates a registration only when the event is active, its deadline has not passed, capacity is available, and the participant has not registered already. Successful registration opens Registered Events and displays a confirmation message. Already Registered appears in green, and a dialog provides participant information, payment state, registration timestamp, and unique pass ID.',
    'The organizer dashboard contains Dashboard, All Events, Registration Reports, and Profile. A New Event action is available from the sidebar. Organizers can perform all event CRUD operations, inspect counts, filter events by category and department, and open members for a selected event. The report screen supports search and event, college, department, and year filters.',
    'The project intentionally excludes participant cancellation and feedback. It records registration payment as PAID for demonstration. A production payment gateway, self-service password recovery, and administrative organizer management are possible later additions rather than current capabilities.'
]
for t in overview:para(t)
heading('Feasibility Study','feasibility',2,new=True)
for sub,text in [
('Technical Feasibility','The required technologies are supported by the current project: Java 17, Maven, JSP, Servlets, JDBC, MySQL, and Tomcat 9. The application uses javax.servlet and therefore targets Tomcat 9. A conventional browser provides the client interface, while JavaMail connects to Gmail SMTP for live email delivery. Local development needs a running MySQL service and an available Tomcat HTTP port.'),
('Operational Feasibility','Participants use familiar form controls and clearly named sidebar sections. Registration and OTP status messages guide onboarding. Organizers use one event form and a report table rather than maintaining separate spreadsheets. Standard college, department, category, and year dropdown values improve consistency during entry and filtering.'),
('Economic Feasibility','The academic solution can be developed with existing college computers and commonly available development tools. It does not require a paid payment service because payment is simulated. Deployment costs depend on future hosting, SMTP usage, database resources, and operational support; the project does not claim a measured cost saving.'),
('Schedule Feasibility','The layered design supports incremental implementation: schema and models, authentication, participant onboarding, event CRUD, event registration, reports, and interface verification. Each module has a limited responsibility, allowing problems to be isolated during development and demonstration.'),
('Data Feasibility','The three core tables represent users, events, and registrations. Primary keys, unique indexes, and foreign keys support consistent identities and relationships. Student and event photos are stored as BLOBs, avoiding filesystem synchronization requirements for the small deployment.'),
('Security and Deployment Feasibility','BCrypt cost 12, session-based authentication, OTP verification, role checks, and prepared statements provide the implemented security foundation. Production readiness would additionally require secret management, rate limiting, CSRF protection, secure HTTPS deployment, and automated tests. The classroom application should be evaluated within that scope.')]:
    heading(sub,level=3);para(text)

heading('TECHNOLOGIES USED','technologies',new=True)
para('The project uses the Java web technology option represented by this template. HTML, CSS, and JavaScript provide the browser interface; JDBC connects to MySQL; Servlets handle requests; and JSP renders views. The alternative framework and database names retained in the contents headings are template labels. React, Node.js, Express, Oracle, and MongoDB are not used by this implementation.')
heading('HTML, CSS & JavaScript (or) React JS','frontend',2)
for sub,text in [
('HTML','HTML structures the login, participant registration, dashboard, event, and report pages. Semantic forms define required fields and submit requests to clean Servlet endpoints. Input types support passwords, email, phone, dates, time, numbers, and file uploads. Dropdowns restrict standard values, while dialog elements display pass details.'),
('CSS','The stylesheet controls spacing, typography, colours, card grids, sidebars, buttons, forms, dialogs, and tables. Responsive rules adjust the layout for smaller screens. The visual design is intentionally simple and professional, with clear contrast and no animation. Success states distinguish registrations that are already complete.'),
('JavaScript','JavaScript validates fields before submission, manages password visibility, displays OTP status, enables submission after successful email verification, controls registration dialogs, and applies event date constraints. Server-side validation repeats critical rules because browser scripts can be bypassed.'),
('Form Validation','Passwords require 6 to 15 characters with a lowercase letter, uppercase letter, number, and special symbol. Confirmation must match. Study year is restricted to 1 through 4. Departments are CSE, IT, AI&DS, CSM, EEE, ECE, MECH, CIVIL, and ASE. College names come from the fixed ten-college list shared with server validation.'),
('Browser Interaction','Participant and organizer sections are reached through /dashboard, /events, /registrations, /reports, and /profile. The browser does not expose JSP filenames as application endpoints. After a successful event registration, a redirect to /registrations follows the POST request so refreshing the list does not repeat the submitted registration.')]:
    heading(sub,level=3);para(text)

heading('JDBC (Oracle or Mongo DB)','jdbc',2,new=True)
for text in [
    'JDBC is the Java API used for relational database access. This application uses MySQL Connector/J rather than Oracle or MongoDB. Dao.getConnection obtains a connection using the configured JDBC URL, username, and password. AppDao contains schema creation, participant lookups, registration uniqueness checks, event operations, image access, and report queries.',
    'PreparedStatement binds request values as parameters. This avoids constructing SQL by concatenating participant names, search text, or other submitted data. ResultSet rows are mapped to User, Event, ParticipantEventRegistration, and RegistrationRecord model objects before they are supplied to the views.',
    'The users table stores roll_no, full_name, email, phone_number, college_name, password_hash, role, department, study_year, gender, address, profile_photo, profile_photo_type, and created_at. The roll number is the primary key; email and phone have unique constraints. Organizer-specific optional student attributes can remain empty.',
    'The events table stores id, title, description, category, department, venue, event_date, registration_closes_at, capacity, fee, image_data, image_type, organizer_id, active, and created_at. The organizer foreign key connects each event to a user account. Event images are streamed from database BLOBs through EventImageServlet.',
    'The registrations table stores id, event_id, participant_id, pass_id, payment_status, and registered_at. A unique constraint on event_id and participant_id prevents repeated registration for one participant and event. pass_id is also unique. Event deletion cascades to its registration records.',
    'The relationship between participants and events is many to many through registrations. One organizer can create many events, and each event can contain many registrations. Registration logic checks availability and uses database constraints and transaction handling to protect consistency. Connections, statements, and result sets are closed with try-with-resources.',
    'Application initialization creates tables when they do not exist, ensures required event columns and phone uniqueness, and inserts the organizer when absent. Database connection values can be supplied through DB_URL, DB_USER, and DB_PASSWORD. Startup failures and database errors are logged for diagnosis.'
]:para(text)

heading('Servlets (or) Node JS & Express JS','servlets',2,new=True)
for text in [
    'Java Servlets form the controller layer; Node.js and Express are not used. Annotation mappings assign clean URL paths to the controllers. doGet generally loads a view or image, while doPost processes form submissions. Controllers obtain session identity, validate request parameters, call AppDao or EmailService, and forward to a JSP or redirect to a route.',
    'Login accepts the account identifier and password and verifies the BCrypt hash. A valid participant can proceed directly to a dashboard. A valid organizer receives an email OTP, and OtpServlet completes the pending login only after verification. LogoutServlet invalidates the session.',
    'RegisterServlet accepts multipart participant registration, validates mandatory values, checks verified email ownership, enforces uniqueness, reads the profile image, hashes the password, and saves the user. RegistrationEmailOtpServlet sends and verifies the email code and stores temporary verification state in the session.',
    'DashboardServlet provides the role-specific dashboard and selects the available, registered, or profile section according to the endpoint. It applies category and department filters to the appropriate event list. EventServlet loads details and accepts organizer create, update, and delete operations, including image uploads and date validation.',
    'RegistrationServlet registers participants for events, creates pass information through the DAO, sends the confirmation email, and redirects successful requests to Registered Events. ReportServlet supplies organizer reports with search and dropdown filters. EventImageServlet and ProfilePhotoServlet stream image bytes with their stored content types.',
    'AuthFilter protects authenticated routes. The application stores the authenticated User in HttpSession and sets cache-control headers on protected responses. web.xml configures a 30-minute session timeout, an HttpOnly session cookie, and cookie-based session tracking. AppContextListener initializes the database at startup.',
    'The project is packaged with mvn package as target/campus-events.war and deployed under the campus-events context on Tomcat 9. The HTTP port is chosen in the Tomcat or Eclipse configuration. MySQL and SMTP connectivity must be available for the corresponding workflows.'
]:para(text)

heading('JSP','jsp',2,new=True)
for text in [
    'JavaServer Pages provide the server-rendered view layer. JSP pages receive request attributes prepared by a Servlet and use the authenticated session user to present role-specific information. The views are stored under WEB-INF/views so the application exposes controller routes rather than direct JSP filenames.',
    'login.jsp displays the login form, password visibility control, Reset, Login, and participant registration link. register.jsp contains the complete participant form, email OTP controls, password requirements, show-password checkbox, profile image upload, Submit, and Reset. verify-otp.jsp completes organizer authentication.',
    'userdashboard.jsp implements the participant sidebar and sections. Available Events and Registered Events each use their own GET filter form. Registration dialogs display name, roll number, email, phone, pass ID, payment status, and registration timestamp. Profile includes the photograph and all participant details.',
    'organizerdashboard.jsp implements organizer summaries, all-event cards, filters, registration reports, and profile. event-form.jsp renders the organizer event form or participant event detail view according to the role. Back links preserve the intended event-list destination.',
    'Model objects separate retrieved data from page layout. Event carries the event details and registration count. User represents account and profile data. ParticipantEventRegistration combines an event and its pass information, and RegistrationRecord represents organizer report rows.',
    'The UI follows a simple hierarchy with consistent buttons, field labels, error messages, and sidebar states. Descriptions remain on the details screen rather than crowding cards. Uploaded images are loaded through protected endpoints. Required fields and dropdowns reduce incomplete and inconsistent data entry.'
]:para(text)

heading('WEB APPLICATION ARCHITECTURE','architecture',new=True)
p=doc.add_paragraph();clean_p(p,align=WD_ALIGN_PARAGRAPH.CENTER,indent=False)
p.add_run().add_picture(str(IMAGE),width=Inches(6.25))
p=doc.add_paragraph('Figure 1  Campus Event Management System architecture and workflow')
clean_p(p,align=WD_ALIGN_PARAGRAPH.CENTER,indent=False)

heading('CODING & IMPLEMENTATION','coding',new=True)
para('The following listings contain every Java source file in the current src/main/java directory. Each listing includes the filename and package path and preserves the source order and indentation. The SMTP app password and database password literal are masked in this report; the working application source has not been altered. The project uses Java 17, javax.servlet, MySQL Connector/J, JavaMail, and jBCrypt.')
para('The implementation is organized into com.model for data objects, com.dao for JDBC operations and schema initialization, com.util for password hashing, com.service for email, and com.servlets for request handling, authentication, images, reports, and startup. Each file starts on a separate page for clear identification.')
files=sorted((ROOT/'src/main/java').rglob('*.java'),key=lambda p:(p.parent.name,p.name))
for index,path in enumerate(files,1):
    heading(str(index)+'  '+path.name,level=2,new=True)
    p=doc.add_paragraph(path.relative_to(ROOT).as_posix());clean_p(p,indent=False)
    source=path.read_text(encoding='utf-8-sig')
    source=re.sub(r'(SMTP_APP_PASSWORD\s*=\s*)"[^"]+"',r'\1"[SMTP APP PASSWORD]"',source)
    source=re.sub(r'(env\("DB_PASSWORD",\s*)"[^"]+"',r'\1"[DATABASE PASSWORD]"',source)
    source_lines=source.splitlines()
    for line_index,line in enumerate(source_lines):
        p=doc.add_paragraph()
        clean_p(p,indent=False)
        pf=p.paragraph_format;pf.space_before=Pt(0);pf.space_after=Pt(0);pf.line_spacing=1
        pf.widow_control=False
        if len(source_lines)-12 <= line_index < len(source_lines)-1:
            pf.keep_with_next=True
        # Each source line is its own paragraph: justification leaves final lines natural.
        # Hanging indent keeps wrapped continuations distinct from source indentation.
        spaces=len(line)-len(line.lstrip(' '))
        pf.left_indent=Inches(min(spaces*.045,2.1))
        pf.right_indent=Pt(0)
        font(p.add_run(line.lstrip(' ') if line else ''))

heading('RESULTS','results',new=True)
# Two reserved pages including this heading, with no fabricated screenshots or outcomes.
page()
p=doc.add_paragraph();clean_p(p,indent=False)
heading('CONCLUSION','conclusion',new=True)
for text in [
    'The Campus Event Management System provides a practical full stack solution for campus event publication and participant registration. Its participant and organizer roles support email verification, authenticated sessions, event browsing, controlled registration, pass retrieval, event maintenance, and registration reports in one interface.',
    'Unique roll numbers, email addresses, and phone numbers improve identity consistency. BCrypt cost 12 protects stored passwords. The session filter and Servlet role checks restrict access, while prepared statements and relational constraints support safe and consistent database operations. OTP email messages and registration receipts complete the main communication workflows.',
    'The application separates browser presentation, Servlet controllers, models, data access, and email services. This structure makes the project suitable for demonstrating HTML, CSS, JavaScript, JSP, Servlets, JDBC, MySQL, and Tomcat together. Category and department filters and detailed reports help both participant navigation and organizer administration.',
    'The current system treats payment as a classroom simulation and records PAID without a real gateway. Future work can add payment verification, password recovery, administrative organizer management, QR-based attendance, downloadable reports, automated tests, and production deployment hardening. Cancellation and feedback remain outside the current project scope.'
]:para(text)
heading('REFERENCES','references',new=True)
for text in [
    'Oracle. Java Platform Standard Edition 17 API Specification. https://docs.oracle.com/en/java/javase/17/docs/api/',
    'Oracle. JDBC API and JDBC documentation. https://docs.oracle.com/javase/8/docs/technotes/guides/jdbc/',
    'Apache Software Foundation. Apache Tomcat 9 Documentation. https://tomcat.apache.org/tomcat-9.0-doc/',
    'Oracle. MySQL 8 Reference Manual. https://dev.mysql.com/doc/refman/8.0/en/',
    'Apache Software Foundation. Maven Documentation. https://maven.apache.org/guides/',
    'MDN Web Docs. HTML, CSS, and JavaScript documentation. https://developer.mozilla.org/en-US/docs/Web',
    'Eclipse Foundation. JavaMail project documentation. https://javaee.github.io/javamail/',
    'jBCrypt. Java implementation of BCrypt password hashing. https://www.mindrot.org/projects/jBCrypt/',
    'Campus Event Management System. Current project source code and README.md. Local project implementation.'
]:para(text)

# Normalize all runs, including inherited fields and table content, to the requested family.
for part in [doc.element,sec.header._element,sec.footer._element]:
    for r in part.xpath('.//w:r'):
        from docx.text.run import Run
        run=Run(r,None)
        size=run.font.size.pt if run.font.size else 12
        font(run,size if size in (12,14,16) else 12)
# Paragraph marks and inherited style font themes must use the same font system.
for part in [doc.styles.element,doc.element,sec.header._element,sec.footer._element]:
    for rpr in part.xpath('.//w:rPr'):
        rf=rpr.find(qn('w:rFonts'))
        if rf is None: rf=OxmlElement('w:rFonts');rpr.insert(0,rf)
        for a in list(rf.attrib):
            if 'theme' in a.lower():del rf.attrib[a]
        for a in ('ascii','hAnsi','cs','eastAsia'):rf.set(qn('w:'+a),'Times New Roman')
        for tag in ('sz','szCs'):
            sz=rpr.find(qn('w:'+tag))
            if sz is not None and sz.get(qn('w:val')) not in ('24','28','32'):
                sz.set(qn('w:val'),'24')
settings=doc.settings.element
uf=settings.find(qn('w:updateFields'))
if uf is None: uf=OxmlElement('w:updateFields');settings.append(uf)
uf.set(qn('w:val'),'true')
doc.core_properties.title=TITLE+' Project Report'
doc.core_properties.subject='FSD II project report'
doc.core_properties.author=''
OUT.parent.mkdir(exist_ok=True)
doc.save(OUT)
(ROOT/'target/report_manifest.json').write_text(json.dumps({'reference_sha256':hashlib.sha256(REF.read_bytes()).hexdigest(),'output':str(OUT),'bookmarks':bookmarks,'java_files':[str(p.relative_to(ROOT)) for p in files],'toc_headings':[r[0] for r in toc_rows]},indent=2),encoding='utf-8')
print(OUT)
print('Java files:',len(files),'Paragraphs:',len(doc.paragraphs))
