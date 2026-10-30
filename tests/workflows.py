"""HTTP regression checks. Run through run-workflow-check.ps1 against an isolated DB."""

import concurrent.futures
import html
import http.cookiejar
import re
import sys
import urllib.error
import urllib.parse
import urllib.request


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, request, response, code, message, headers, url):
        return None


class Client:
    def __init__(self):
        self.cookies = http.cookiejar.CookieJar()
        self.opener = urllib.request.build_opener(
            urllib.request.HTTPCookieProcessor(self.cookies), NoRedirect()
        )

    def request(self, path, fields=None, files=None):
        headers = {}
        data = None
        if files is not None:
            boundary = 'CampusWorkflowBoundary'
            parts = []
            for name, value in (fields or {}).items():
                parts.append(
                    f'--{boundary}\r\nContent-Disposition: form-data; name="{name}"'
                    f'\r\n\r\n{value}\r\n'.encode()
                )
            for name, (filename, mime, content) in files.items():
                parts.append(
                    f'--{boundary}\r\nContent-Disposition: form-data; name="{name}"; '
                    f'filename="{filename}"\r\nContent-Type: {mime}\r\n\r\n'.encode()
                    + content + b'\r\n'
                )
            data = b''.join(parts) + f'--{boundary}--\r\n'.encode()
            headers['Content-Type'] = f'multipart/form-data; boundary={boundary}'
        elif fields is not None:
            data = urllib.parse.urlencode(fields).encode()
        request = urllib.request.Request(BASE + path, data=data, headers=headers)
        try:
            response = self.opener.open(request, timeout=30)
        except urllib.error.HTTPError as error:
            response = error
        return response.code, response.headers, response.read().decode('utf-8', errors='replace')

    def login(self, roll, password='Password@123'):
        return self.request('/login', {'rollNo': roll, 'password': password})


COUNT = 0


def check(condition, message):
    global COUNT
    if not condition:
        raise AssertionError(message)
    COUNT += 1
    print(f'PASS {COUNT}: {message}', flush=True)


def redirect(result, destination):
    return result[0] == 302 and result[1].get('Location') == '/campus-events' + destination


def participant(index=1, **changes):
    values = dict(
        fullName='QA Participant <script>alert(1)</script> శ్రీ',
        rollNo=f'QATEST00{index}', email=f'qa{index}@campus.test',
        phoneNumber=f'987650000{index}',
        collegeName='Lakireddy Bali Reddy college of Engineering',
        department='CSE', year='3', gender='Male', address='Campus & town',
        password='Password@123', confirmPassword='Password@123',
    )
    values.update(changes)
    return values


# A real one-pixel PNG: both upload and image-serving paths are exercised.
import base64
PNG = base64.b64decode(
    'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aXioAAAAASUVORK5CYII='
)
PHOTO = {'profilePhoto': ('profile.png', 'image/png', PNG)}
IMAGE = {'eventImage': ('event.png', 'image/png', PNG)}


def main():
    anonymous = Client()
    for path in ('/', '/login', '/register', '/assets/style.css', '/assets/validation.js'):
        check(anonymous.request(path)[0] == 200, f'Public page/asset {path}')
    for path in ('/dashboard', '/events', '/registrations', '/profile', '/event',
                 '/reports', '/event-image?id=1', '/profile-photo'):
        check(redirect(anonymous.request(path), '/login?status=session-required'),
              f'Anonymous access protected: {path}')
    for path in ('/verify-otp', '/registration-email-otp'):
        check(anonymous.request(path)[0] == 404, f'Removed verification route {path}')
    result = anonymous.login('ORG001', 'wrong')
    check(result[0] == 200 and 'Invalid roll number or password' in result[2], 'Wrong password rejected')
    page = anonymous.request('/register')[2]
    check('Send OTP' not in page and 'type="submit" disabled' not in page, 'Registration needs no OTP')

    invalid = [
        (dict(email='invalid'), 'valid email'),
        (dict(password='weak', confirmPassword='weak'), 'Password must be'),
        (dict(confirmPassword='Different@1'), 'Passwords do not match'),
        (dict(department='INVALID'), 'valid department'),
        (dict(year='5'), 'valid study year'),
        (dict(phoneNumber='123'), 'valid 10-digit'),
        (dict(fullName=''), 'Full name is required'),
        (dict(collegeName='Unknown'), 'valid college'),
    ]
    for changes, expected in invalid:
        result = anonymous.request('/register', participant(**changes), PHOTO)
        check(result[0] == 200 and expected in result[2], f'Registration rejects {changes}')
    result = anonymous.request('/register', participant(), {})
    check('Profile photo is required' in result[2], 'Missing photo rejected')
    result = anonymous.request('/register', participant(),
                               {'profilePhoto': ('file.txt', 'text/plain', b'no image')})
    check('valid JPG, PNG or WEBP' in result[2], 'Unsupported photo type rejected')
    result = anonymous.request('/register', participant(),
                               {'profilePhoto': ('large.png', 'image/png', b'x' * 2_097_153)})
    check(result[0] != 500, 'Oversized participant upload handled without server error')

    users = []
    for index in range(1, 5):
        client = Client()
        values = participant(index)
        if index == 4:
            values.update(password=' Password@123 ', confirmPassword=' Password@123 ')
        check(redirect(client.request('/register', values, PHOTO), '/login?status=registered'),
              f'Participant {index} registers without OTP')
        check(redirect(client.login(values['rollNo'].lower(), values['password']), '/dashboard'),
              f'Participant {index} login; password preserved exactly')
        users.append(client)
    for changes, expected in [
        (dict(rollNo='QATEST001'), 'Roll number already exists'),
        (dict(email='QA1@CAMPUS.TEST'), 'Email address already exists'),
        (dict(phoneNumber='9876500001'), 'Phone number already exists'),
    ]:
        result = anonymous.request('/register', participant(9, **changes), PHOTO)
        check(result[0] == 200 and expected in result[2], expected)
    for path in ('/dashboard', '/events', '/registrations', '/profile'):
        check(users[0].request(path)[0] == 200, f'Participant page {path}')
    profile = users[0].request('/profile')
    check('&lt;script&gt;' in profile[2] and '<script>alert(1)</script>' not in profile[2],
          'Participant text safely escaped')
    check('శ్రీ' in profile[2], 'Unicode participant name preserved')
    check('no-store' in profile[1].get('Cache-Control', ''), 'Profile is not cached')
    check(users[0].request('/profile-photo')[0] == 200, 'Own profile photo served')
    check(users[0].request('/profile-photo?rollNo=QATEST002')[0] == 403, 'Other participant photo protected')
    for path in ('/reports', '/event'):
        check(users[0].request(path)[0] == 403, f'Organizer-only route protected {path}')
    check(users[0].request('/event', {'action': 'delete', 'id': '1'})[0] == 403,
          'Participant cannot delete events')

    organizer = Client()
    check(redirect(organizer.login('ORG001', 'Admin@123'), '/dashboard'), 'Organizer login directly without OTP')
    for path in ('/dashboard', '/events', '/reports', '/profile', '/event'):
        check(organizer.request(path)[0] == 200, f'Organizer page {path}')
    check(redirect(organizer.request('/registrations'), '/reports'), 'Organizer registration route redirects to reports')
    check(organizer.request('/registration', {'eventId': '1'})[0] == 403, 'Organizer cannot register as participant')
    event = dict(title='QA <script>alert(2)</script> "Event"', description='QA description <b>text</b>',
                 category='Technical', department='CSE', venue='Lab & Hall',
                 eventDate='2099-12-20', eventTime='10:00',
                 registrationCloseDate='2099-12-19', registrationCloseTime='18:00',
                 capacity='2', fee='200', active='on')
    for changes in [dict(eventDate='invalid'), dict(fee='NaN'), dict(fee='Infinity'),
                    dict(capacity='0'), dict(department='bad'), dict(category='bad'),
                    dict(registrationCloseDate='2099-12-21'), dict(title='x' * 151)]:
        check(organizer.request('/event', dict(event, **changes), IMAGE)[0] == 400,
              f'Invalid event data returns 400: {changes}')
    check(organizer.request('/event', event, {})[0] == 400, 'New event requires image')
    check(organizer.request('/event', event,
                            {'eventImage': ('large.png', 'image/png', b'x' * 5_242_881)})[0] == 400,
          'Oversized event upload returns 400')
    check(redirect(organizer.request('/event', event, IMAGE), '/events'), 'Organizer creates event')
    listing = organizer.request('/events')[2]
    match = re.search(r'href="event\?id=(\d+)"', listing)
    check(match is not None, 'Created event appears in organizer list')
    event_id = match.group(1)
    for path in (f'/event?id={event_id}', f'/event-image?id={event_id}'):
        check(organizer.request(path)[0] == 200, f'Created event endpoint {path}')
    check('&lt;script&gt;' in listing and '<script>alert(2)</script>' not in listing, 'Event text safely escaped')
    check('QA &lt;script&gt;' in users[0].request('/events?category=Technical&department=CSE')[2],
          'Available event category/department filters match')
    check('QA &lt;script&gt;' not in users[0].request('/events?category=Sports')[2], 'Nonmatching filter excludes event')
    check('href="registrations"' in users[0].request(f'/event?id={event_id}&from=registrations')[2],
          'Event detail Back returns to registered events')
    check(redirect(users[0].request('/registration', {'eventId': event_id}), '/registrations'),
          'Registration creates pass and redirects to registered events')
    registered = users[0].request('/registrations')[2]
    check(re.search(r'EVT-\d+-[A-F0-9]{8}', registered) is not None, 'Unique pass appears in registered events')
    check('<dialog' in registered and 'registration-details-button' in registered, 'Registration detail popup markup present')
    check('already registered' in users[0].request('/events')[2].lower(), 'Available event shows Already registered')
    users[0].request('/registration', {'eventId': event_id})
    check('already registered' in users[0].request('/events')[2].lower(), 'Duplicate registration rejected')
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as executor:
        results = list(executor.map(lambda c: c.request('/registration', {'eventId': event_id}), users[1:3]))
    check(sum(redirect(r, '/registrations') for r in results) == 1, 'Concurrent signup cannot exceed capacity')
    result = organizer.request('/event', dict(event, id=event_id, capacity='1'), {})
    check(result[0] == 400, 'Capacity cannot be reduced below registrations')
    check(redirect(organizer.request('/event', dict(event, id=event_id, title='QA Updated Event'), {}), '/events'),
          'Organizer edits event while preserving image')
    check(organizer.request(f'/event-image?id={event_id}')[0] == 200, 'Image preserved after edit')
    report = organizer.request('/reports?eventId=' + event_id + '&department=CSE&year=3&search=QATEST001')
    report_text = html.unescape(re.sub(r'<[^>]+>', '', report[2]))
    check(report[0] == 200 and '1 matching registrations' in report_text, 'Reports combine event, department, year and search')
    result = organizer.request('/reports?search=%22%3E%3Cscript%3Ealert(9)%3C/script%3E')
    check('<script>alert(9)</script>' not in result[2] and '&quot;&gt;&lt;script&gt;' in result[2],
          'Report search cannot inject HTML')
    for path in ('/event?id=bad', '/event-image?id=bad', '/reports?year=bad'):
        check(organizer.request(path)[0] == 400, f'Malformed parameter handled {path}')
    check(organizer.request('/event?id=2147483647')[0] == 404, 'Missing event returns 404')
    check(redirect(organizer.request('/event', {'action': 'delete', 'id': event_id}), '/events'), 'Organizer deletes event')
    check('EVT-' not in users[0].request('/registrations')[2], 'Event deletion cascades to registrations')
    for client in (users[0], organizer):
        check(redirect(client.request('/logout', {}), '/login?status=logged-out'), 'Logout invalidates session')
        check(redirect(client.request('/dashboard'), '/login?status=session-required'), 'Back/protected request after logout requires login')
    print(f'All {COUNT} workflow checks passed.')


if __name__ == '__main__':
    BASE = sys.argv[1].rstrip('/')
    main()
