import librosa
from librosa import feature
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt

import os
import pathlib
import csv

import tensorflow as tf
import tensorflow.keras as keras
from sklearn.preprocessing import LabelEncoder

# Preprocessing
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder, StandardScaler
from sklearn.neighbors import KNeighborsClassifier
from sklearn.metrics import classification_report, confusion_matrix
from tensorflow.python.keras.backend import sparse_categorical_crossentropy
from tensorflow.python.keras.layers.core import Dropout

#Extracting a spectogram from every file
'''
def create_spectograms():
    cmap = plt.get_cmap('inferno')

    plt.figure(figsize=(10,10))
    genres = 'blues classical country disco hiphop jazz metal pop reggae rock'.split()
    for g in genres:
        pathlib.Path(f'img_data/{g}').mkdir(parents=True, exist_ok=True)     
        for filename in os.listdir(f'C:\\Users\\stipa\\Desktop\\Data\\genres_original\\{g}'):
            songname = f'C:\\Users\\stipa\\Desktop\\Data\\genres_original\\{g}/{filename}'
            y, sr = librosa.load(songname, mono=True, duration=5)
            plt.specgram(y, NFFT=2048, Fs=2, Fc=0, noverlap=128, cmap=cmap, sides='default', mode='default', scale='dB')
            plt.axis('off')
            plt.savefig(f'img_data/{g}/{filename[:-3].replace(".", "")}.png')
            plt.clf()
    plt.show()
'''

#Writing to csv file

def generate_features():
    header = 'filename chroma_stft rmse spectral_centroid spectral_bandwidth rolloff zero_crossing_rate'
    for i in range(1, 21):
        header += f' mfcc{i}'
    header += ' label'
    header = header.split()

    file = open('data.csv', 'w', newline='')
    with file:
        writer = csv.writer(file)
        writer.writerow(header)
    genres = 'blues classical country disco hiphop jazz metal pop reggae rock'.split()
    for g in genres:
        for filename in os.listdir(f'C:\\Users\\stipa\\Desktop\\Data\\genres_original\\{g}'):
            songname = f'C:\\Users\\stipa\\Desktop\\Data\\genres_original\\{g}/{filename}'
            y, sr = librosa.load(songname, mono=True, duration=30)
            chroma_stft = librosa.feature.chroma_stft(y=y, sr=sr)
            spec_cent = librosa.feature.spectral_centroid(y=y, sr=sr)
            spec_bw = librosa.feature.spectral_bandwidth(y=y, sr=sr)
            rolloff = librosa.feature.spectral_rolloff(y=y, sr=sr)
            zcr = librosa.feature.zero_crossing_rate(y)
            mfcc = librosa.feature.mfcc(y=y, sr=sr)
            rmse = librosa.feature.rms(y=y)
            to_append = f'{filename} {np.mean(chroma_stft)} {np.mean(rmse)} {np.mean(spec_cent)} {np.mean(spec_bw)} {np.mean(rolloff)} {np.mean(zcr)}'    
            for e in mfcc:
                to_append += f' {np.mean(e)}'
            to_append += f' {g}'
            file = open('data.csv', 'a', newline='')
            with file:
                writer = csv.writer(file)
                writer.writerow(to_append.split())


def get_feature_data(datacsv):
    data = pd.read_csv(datacsv)
    shuffled_data = data.sample(frac=1)
    y = shuffled_data.label
    x = shuffled_data.drop(["label", "filename"], axis=1)

    le = LabelEncoder()
    target = le.fit_transform(y)

    #print(y)
    #print(target)
    return x,target

def split_data(x, y):
    x_train,x_test,y_train,y_test=train_test_split(x,y,test_size=0.3)
    #print(x_train.shape)
    #print(x_test.shape)

    scaler = StandardScaler()
    scaler.fit(x_train)

    X_train = scaler.transform(x_train)
    X_test = scaler.transform(x_test)

    return X_train, X_test, y_train, y_test

#kernel_regularizer=keras.regularizers.l2(0.001) - keras.layers.Dropout(0.4)

def create_model(X_train):
    model = keras.Sequential([
        keras.layers.Flatten(input_shape=(X_train.shape[1],)),

        keras.layers.Dense(512, activation="relu", kernel_regularizer=keras.regularizers.l2(0.001)),
        keras.layers.Dropout(0.4),

        keras.layers.Dense(256, activation="relu", kernel_regularizer=keras.regularizers.l2(0.001)),
        keras.layers.Dropout(0.4),

        keras.layers.Dense(64, activation="relu", kernel_regularizer=keras.regularizers.l2(0.001)),
        keras.layers.Dropout(0.4),

        keras.layers.Dense(10, activation="softmax")
    ])

    return model

def create_overfitting_graph(history):
    fig, axs = plt.subplots(2)

    axs[0].plot(history.history["sparse_categorical_accuracy"], label="train_accuracy")
    axs[0].plot(history.history["val_sparse_categorical_accuracy"], label="test_accuracy")
    axs[0].set_ylabel("Accuracy")
    axs[0].legend(loc="lower right")
    axs[0].set_title("Accuracy eval")

    axs[1].plot(history.history["loss"], label="train_error")
    axs[1].plot(history.history["val_loss"], label="test_error")
    axs[1].set_ylabel("Error")
    axs[1].set_xlabel("Epoch")
    axs[1].legend(loc="upper right")
    axs[1].set_title("Error eval")

    plt.show()

def create_tensorflowlite_file(model):
    TF_LITE_MODEL_NAME = "tf_lite_model.tflite"
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    tflite_model = converter.convert()

    open(TF_LITE_MODEL_NAME, "wb").write(tflite_model)

def generate_features(song):
    y, sr = librosa.load(song, mono=True, duration=30)
    chroma_stft = librosa.feature.chroma_stft(y=y, sr=sr)
    spec_cent = librosa.feature.spectral_centroid(y=y, sr=sr)
    spec_bw = librosa.feature.spectral_bandwidth(y=y, sr=sr)
    rolloff = librosa.feature.spectral_rolloff(y=y, sr=sr)
    zcr = librosa.feature.zero_crossing_rate(y)
    mfcc = librosa.feature.mfcc(y=y, sr=sr)
    rmse = librosa.feature.rms(y=y)

    predict_features = np.zeros((1, 26))
    features = []
    features.append(np.mean(chroma_stft)) 
    features.append(np.mean(rmse)) 
    features.append(np.mean(spec_cent))
    features.append(np.mean(spec_bw))
    features.append(np.mean(rolloff))
    features.append(np.mean(zcr))
    for e in mfcc:
        features.append(np.mean(e))

    for i in range(len(features)):
        predict_features[0][i] = features[i]

    print(predict_features)
    return predict_features


def setupModel(data):
    X, Y = get_feature_data(data)
    X_train, X_test, Y_train, Y_test = split_data(X,Y)
    
    
    '''classifier = KNeighborsClassifier(n_neighbors=5)
    classifier.fit(X_train, Y_train)

    y_pred = classifier.predict(X_test)

    print(confusion_matrix(Y_test, y_pred))
    print(classification_report(Y_test, y_pred))'''

    
    model = create_model(X_train)

    optimizer = keras.optimizers.Adam(learning_rate = 0.001)
    model.compile(  optimizer=optimizer,
                    loss="sparse_categorical_crossentropy",
                    metrics=["sparse_categorical_accuracy"])

    model.summary()

    history = model.fit(X_train, Y_train, validation_data=(X_test, Y_test), epochs=50, batch_size=32)

    return model

def full_prediction(song, model):
    '''
    #print("Evaluate on test data")
    #results = model.evaluate(X_test, Y_test, batch_size=128)
    #print("test loss, test acc:", results)

    #print(X_test.shape)

    #create_overfitting_graph(history)
    topredict = generate_features(song)
    #print(topredict.shape)
    prediction = np.argmax(model.predict(topredict), axis=-1)
    #print(prediction)

    
    #create_tensorflowlite_file(model)'''

    topredict = generate_features(song)
    prediction = model.predict(topredict)
    print(prediction)
    return prediction

if __name__ == "__main__":
    full_prediction("C:\\Users\\stipa\\Desktop\\CLAUDE DEBUSSY -  CLAIR DE LUNE.wav", setupModel("data.csv"))