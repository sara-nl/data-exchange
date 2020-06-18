#!/usr/bin/env python3
from __future__ import print_function

from os import path
import os.path
import sys
from keras.preprocessing.image import array_to_img
from keras.preprocessing.image import img_to_array
from keras.preprocessing.image import load_img
from tensorflow.keras.preprocessing.image import ImageDataGenerator
import numpy as np
import tensorflow as tf

if len(sys.argv) < 2:
    print("ERROR: Expected an image path as the command line argument")
    sys.exit(1)

test_image = sys.argv[1]
model_file = os.path.join(os.path.dirname(
    os.path.abspath(__file__)), 'catdog.h5')

if (not path.isfile(test_image)):
    print("ERROR: Expected %s to be a file" % test_image, file=sys.stderr)
    sys.exit(1)


model = tf.keras.models.load_model(model_file)


img = load_img(test_image, target_size=(150, 150))
img_tensor = img_to_array(img)

img_tensor = np.expand_dims(img_tensor, axis=0)
img_tensor /= 255.

classes = ['cat 😺', 'dog 🐶']
klass = model.predict_classes(img_tensor, batch_size=1)[0][0]


print("This is (probably) a %s" % classes[klass])
