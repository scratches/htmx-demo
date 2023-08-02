from django.http import HttpResponse
from django.template import loader
import datetime
import time
from django.http import StreamingHttpResponse

def stream(request):
    def event_stream():
        while True:
            time.sleep(3)
            yield 'data: The server time is: %s\n\n' % datetime.datetime.now()
    return StreamingHttpResponse(event_stream(), content_type='text/event-stream')

def home(request):
    template = loader.get_template('index.html')
    return HttpResponse(template.render())