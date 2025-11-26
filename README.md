# Multithreaded Musical Player - SEG2105 Assignment 4

A Java application that demonstrates concurrency, thread synchronization, and shared resource management by coordinating two threads to play musical sequences.

## 📝 Overview

This project was developed (likely for **SEG2105 Assignment #4**) to demonstrate how multiple threads can cooperate to perform a synchronized task. The application uses a custom `Sequencer` to manage turn-taking between two player threads, ensuring notes are played in the correct order with specific timing intervals.

## 🎵 Features

The application executes two distinct musical tasks:

### Task 1: The Scale (Synchronization Test)
* **Sequence:** Do, Re, Mi, Fa, Sol, La, Si, Do-Octave.
* **Behavior:** Thread 1 and Thread 2 alternate playing notes sequentially.
* **Timing:** Specific delays are implemented to ensure the correct rhythm
* **Grand Finale:** The final note ("do-octave") is played by **both** threads simultaneously.

### Task 2: Twinkle Twinkle Little Star
* **Sequence:** The classic melody divided into three lines.
* **Behavior:** Notes are pre-assigned to specific threads (Thread 1 or Thread 2) to reconstruct the song seamlessly using the shared sequencer.

---

## 📂 Project Structure

```text
.
├── SimpleMusicalPlayer.java   # Main class containing logic, threading, and Sequencer
├── FilePlayer.java            # Utility class for playing .wav audio files
├── Sounds/                    # Directory containing audio assets
│   ├── do.wav
│   ├── re.wav
│   ├── mi.wav
│   ├── fa.wav
│   ├── sol.wav
│   ├── la.wav
│   ├── si.wav
│   └── do-octave.wav
└── README.md
```

## 🛠️ Technical Details
Language: Java

Concurrency: Uses Thread, Runnable, and the monitor pattern with synchronized blocks.

Coordination: Utilizes lock.wait() and lock.notifyAll() within the Sequencer class to handle precise, turn-based thread execution.

Timing: The Sequencer manages rhythm by holding the synchronization lock (lock) while executing Thread.sleep() between notes.

Audio: Uses javax.sound.sampled via the FilePlayer class to play .wav files.

## 🚀 How to Run
Prerequisites
Java Development Kit (JDK) 8 or higher.

The Sounds directory must be located in the same directory where you execute the compiled code.

### Compilation
Navigate to the source directory in your terminal and compile the Java files:

```
javac SimpleMusicalPlayer.java FilePlayer.java
```

### Execution
Run the main class:

```
java SimpleMusicalPlayer
```
The application will print the sequence of notes being played by Thread-1 and Thread-2 to the console.

## Demo Video
https://github.com/user-attachments/assets/e477c8bf-c813-4a00-a56c-4089e560daf1

