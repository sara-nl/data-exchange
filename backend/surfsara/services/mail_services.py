from django.template.loader import render_to_string
from django.core.mail import EmailMultiAlternatives


def send_mail(mail_files, receiver, subject, options):
    text_body = render_to_string(f"../templates/{mail_files}.txt", options)
    html_body = render_to_string(f"../templates/{mail_files}.html", options)

    message = EmailMultiAlternatives(subject, text_body, to=[receiver])
    message.attach_alternative(html_body, "text/html")
    message.send()