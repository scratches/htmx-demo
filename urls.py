from django.urls import path
import views

urlpatterns = [
    path('', views.home, name='home'),
    path('stream', views.stream, name='stream'),
]