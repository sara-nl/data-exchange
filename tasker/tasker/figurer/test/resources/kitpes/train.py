#!/usr/bin/env python3

from __future__ import absolute_import, division, print_function, unicode_literals

import tensorflow as tf

from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import Dense, Conv2D, Flatten, Dropout, MaxPooling2D
from tensorflow.keras.preprocessing.image import ImageDataGenerator

import os
import numpy as np
import matplotlib.pyplot as plt


def create_model(img_h, img_w):
    return Sequential([
        Conv2D(16, 3, padding='same', activation='relu',
               input_shape=(img_h, img_w, 3)),
        MaxPooling2D(),
        Conv2D(32, 3, padding='same', activation='relu'),
        MaxPooling2D(),
        Conv2D(64, 3, padding='same', activation='relu'),
        MaxPooling2D(),
        Flatten(),
        Dense(512, activation='relu'),
        Dense(1, activation='sigmoid')
    ])


_URL = 'https://storage.googleapis.com/mledu-datasets/cats_and_dogs_filtered.zip'

path_to_zip = tf.keras.utils.get_file(
    'cats_and_dogs.zip', origin=_URL, extract=True)

print("Downloaded images into %s" % path_to_zip)

PATH = os.path.join(os.path.dirname(path_to_zip), 'cats_and_dogs_filtered')

train_dir = os.path.join(PATH, 'train')
validation_dir = os.path.join(PATH, 'validation')

# directory with our training cat pictures
train_cats_dir = os.path.join(train_dir, 'cats')
# directory with our training dog pictures
train_dogs_dir = os.path.join(train_dir, 'dogs')
# directory with our validation cat pictures
validation_cats_dir = os.path.join(validation_dir, 'cats')
# directory with our validation dog pictures
validation_dogs_dir = os.path.join(validation_dir, 'dogs')


train_image_generator = ImageDataGenerator(
    rescale=1./255)  # Generator for our training data
validation_image_generator = ImageDataGenerator(
    rescale=1./255)  # Generator for our validation data

batch_size = 128
epochs = 14
IMG_HEIGHT = 150
IMG_WIDTH = 150

train_data_gen = train_image_generator.flow_from_directory(
    batch_size=batch_size,
    directory=train_dir,
    shuffle=True,
    target_size=(IMG_HEIGHT, IMG_WIDTH),
    class_mode='binary')

val_data_gen = validation_image_generator.flow_from_directory(
    batch_size=batch_size,
    directory=validation_dir,
    target_size=(IMG_HEIGHT, IMG_WIDTH),
    class_mode='binary')


num_cats_tr = len(os.listdir(train_cats_dir))
num_dogs_tr = len(os.listdir(train_dogs_dir))

num_cats_val = len(os.listdir(validation_cats_dir))
num_dogs_val = len(os.listdir(validation_dogs_dir))

total_train = num_cats_tr + num_dogs_tr
total_val = num_cats_val + num_dogs_val

print('total training cat images:', num_cats_tr)
print('total training dog images:', num_dogs_tr)

print('total validation cat images:', num_cats_val)
print('total validation dog images:', num_dogs_val)
print("--")
print("Total training images:", total_train)
print("Total validation images:", total_val)


model = create_model(IMG_HEIGHT, IMG_WIDTH)

model.compile(optimizer='adam', loss='binary_crossentropy',
              metrics=['accuracy'])


model.summary()

print("Training model")

history = model.fit_generator(
    train_data_gen,
    steps_per_epoch=total_train // batch_size,
    epochs=epochs,
    validation_data=val_data_gen,
    validation_steps=total_val // batch_size
)

model.save('catdog.h5')

print("Finised training. Now run validate.py")
