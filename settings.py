from pathlib import Path
BASE_DIR = Path(__file__).resolve().parent.parent
DEBUG = True
SECRET_KEY = '4l0ngs3cr3tstr1ngw3lln0ts0l0ngw41tn0w1tsl0ng3n0ugh'
ROOT_URLCONF = 'urls'
INSTALLED_APPS = [
    'django.contrib.staticfiles',
    'apps'
]
TEMPLATES = [
    {
        'BACKEND': 'django.template.backends.django.DjangoTemplates',
        'DIRS': [],
        'APP_DIRS': True,
    },
]
STATIC_URL = '/static/'
