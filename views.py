from django.http import HttpResponse
from django.template import loader
import datetime
import time
from django.http import StreamingHttpResponse

def stream(request):
    def event_stream():
        template = loader.get_template('time.html')
        value = 0
        while True:
            time.sleep(3)
            value = value + 1
            yield 'data:<div>Index: %d, Time: %s</div>\n\n' % (value, datetime.datetime.now().isoformat())
    return StreamingHttpResponse(event_stream(), content_type='text/event-stream')

def home(request):
    template = loader.get_template('index.html')
    return HttpResponse(template.render())