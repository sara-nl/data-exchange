from django.template.loader import render_to_string
from django.core.mail import EmailMultiAlternatives


def send_mail(mail_files, receiver, subject, **kwargs):
    text_body = render_to_string(f"../templates/{mail_files}.txt", kwargs)
    html_body = render_to_string(f"../templates/{mail_files}.html", kwargs)

    message = EmailMultiAlternatives(subject, text_body, to=[receiver])
    message.attach_alternative(html_body, "text/html")
    print(message)
    message.send()
