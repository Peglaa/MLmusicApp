from typing import List
import librosa
import numpy as np

def generate_features(song):
    y, sr = librosa.load(song, mono=True, duration=30)
    chroma_stft = librosa.feature.chroma_stft(y=y, sr=sr)
    spec_cent = librosa.feature.spectral_centroid(y=y, sr=sr)
    spec_bw = librosa.feature.spectral_bandwidth(y=y, sr=sr)
    rolloff = librosa.feature.spectral_rolloff(y=y, sr=sr)
    zcr = librosa.feature.zero_crossing_rate(y)
    mfcc = librosa.feature.mfcc(y=y, sr=sr)
    rmse = librosa.feature.rms(y=y)

    features = []
    features.append(np.mean(chroma_stft)) 
    features.append(np.mean(rmse)) 
    features.append(np.mean(spec_cent))
    features.append(np.mean(spec_bw))
    features.append(np.mean(rolloff))
    features.append(np.mean(zcr))
    for e in mfcc:
        features.append(np.mean(e))

    print(features)
    return features
