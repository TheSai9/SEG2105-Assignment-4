/**
 * Simple MusicPlayer implementation for SEG2105 Assignment #4
 *
 * This implementation uses a shared sequencer object to coordinate
 * between two threads playing musical notes.
 */
public class SimpleMusicalPlayer {

    private FilePlayer filePlayer;
    private Sequencer sequencer;

    public SimpleMusicalPlayer() {
        filePlayer = new FilePlayer();
        sequencer = new Sequencer();
    }

    /**
     * Task 1: Play the do-re-mi scale with synchronized threads
     * Heard sequence: do, re, mi, fa, sol, la, si, do-octave
     */
    public void playScale() {
        String[] notes = {"do", "re", "mi", "fa", "sol", "la", "si", "do-octave"};
        // 0 = both threads play simultaneously
        int[] threadIds = {1, 2, 1, 2, 1, 2, 1, 0}; 

        sequencer.setSequence(notes, threadIds);
        playWithThreads();
    }

    /**
     * Task 2: Play Twinkle Twinkle Little Star
     */
    public void playTwinkleTwinkleLittleStar() {
        // Line 1
        String[] line1_notes = {"do", "do", "sol", "sol", "la", "la", "sol", "fa", "fa", "mi", "mi", "re", "re", "do"};
        int[] line1_threads = {1, 1, 1, 1, 2, 2, 1, 2, 2, 1, 1, 2, 2, 1};

        // Line 2
        String[] line2_notes = {"sol", "sol", "fa", "fa", "mi", "mi", "re", "sol", "sol", "fa", "fa", "mi", "mi", "re"};
        int[] line2_threads = {1, 1, 2, 2, 1, 1, 2, 1, 1, 2, 2, 1, 1, 2};

        // Line 3
        String[] line3_notes = {"do", "do", "sol", "sol", "la", "la", "sol", "fa", "fa", "mi", "mi", "re", "re", "do"};
        int[] line3_threads = {1, 1, 1, 1, 2, 2, 1, 2, 2, 1, 1, 2, 2, 1};

        // Combine all
        String[] allNotes = new String[line1_notes.length + line2_notes.length + line3_notes.length];
        int[] allThreads = new int[line1_threads.length + line2_threads.length + line3_threads.length];

        int idx = 0;
        for (int i = 0; i < line1_notes.length; i++) { allNotes[idx] = line1_notes[i]; allThreads[idx] = line1_threads[i]; idx++; }
        for (int i = 0; i < line2_notes.length; i++) { allNotes[idx] = line2_notes[i]; allThreads[idx] = line2_threads[i]; idx++; }
        for (int i = 0; i < line3_notes.length; i++) { allNotes[idx] = line3_notes[i]; allThreads[idx] = line3_threads[i]; idx++; }

        sequencer.setSequence(allNotes, allThreads);
        playWithThreads();
    }

    private void playWithThreads() {
        Thread thread1 = new Thread(new PlayerThread(1, filePlayer, sequencer), "Thread-1");
        Thread thread2 = new Thread(new PlayerThread(2, filePlayer, sequencer), "Thread-2");

        thread1.start();
        thread2.start();

        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SimpleMusicalPlayer player = new SimpleMusicalPlayer();

        System.out.println("=== Playing do-re-mi scale (Task 1) ===");
        player.playScale();

        try { Thread.sleep(2000); } catch (InterruptedException e) { e.printStackTrace(); }

        System.out.println("\n=== Playing Twinkle Twinkle Little Star (Task 2) ===");
        player = new SimpleMusicalPlayer();
        player.playTwinkleTwinkleLittleStar();
    }

    /**
     * Inner class: Manages the sequence of notes with proper turn-based coordination.
     */
    private static class Sequencer {
        private String[] notes;
        private int[] threadIds;
        private int currentIndex;
        private final Object lock = new Object();

        private static final long WAIT_TIMEOUT = 2000;
        private static final long NOTE_DELAY = 600;

        // For the simultaneous do-octave:
        private boolean bothPlayFlag = false;
        private boolean bothPlayedDone = false;
        private int bothPlayCount = 0;

        public void setSequence(String[] notes, int[] threadIds) {
            this.notes = notes;
            this.threadIds = threadIds;
            this.currentIndex = 0;
            this.bothPlayFlag = false;
            this.bothPlayedDone = false;
            this.bothPlayCount = 0;
        }

        public String getNextNote(int threadNum) {
            synchronized (lock) {
                // Done playing?
                if (currentIndex >= notes.length && !bothPlayFlag) return null;
                if (bothPlayedDone) return null;

                long startTime = System.currentTimeMillis();

                while (true) {
                    // 1. Handle Simultaneous Mode (Execution Phase)
                    if (bothPlayFlag) {
                        if (bothPlayCount < 2) {
                            bothPlayCount++;
                            if (bothPlayCount == 2) {
                                bothPlayedDone = true;
                            }
                            lock.notifyAll();
                            return "do-octave";
                        } else {
                            return null;
                        }
                    }

                    if (currentIndex >= notes.length) return null;

                    int assignedThread = threadIds[currentIndex];

                    // 2. Detect transition to Simultaneous Mode (ID 0)
                    // This block runs AFTER 'si' is finished and currentIndex is incremented.
                    if (assignedThread == 0 && "do-octave".equals(notes[currentIndex])) {
                        
                        // --- HARDCODED GAP FOR THE FINALE ---
                        // This adds a delay before the simultaneous "do-octave" begins.
                        try {
                            Thread.sleep(1000); 
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        // ------------------------------------

                        bothPlayFlag = true;
                        bothPlayCount = 0;
                        lock.notifyAll(); 
                        continue; 
                    }

                    // 3. Normal Single Thread Mode
                    if (assignedThread == threadNum) {
                        String note = notes[currentIndex];
                        currentIndex++;

                        try {
                            Thread.sleep(NOTE_DELAY);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return null;
                        }

                        lock.notifyAll(); 
                        return note;
                    }

                    // 4. Not my turn
                    try {
                        lock.wait(50);
                        if (System.currentTimeMillis() - startTime > WAIT_TIMEOUT) return null;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }
            }
        }
    }

    private static class PlayerThread implements Runnable {
        private int threadNum;
        private FilePlayer filePlayer;
        private Sequencer sequencer;

        public PlayerThread(int threadNum, FilePlayer filePlayer, Sequencer sequencer) {
            this.threadNum = threadNum;
            this.filePlayer = filePlayer;
            this.sequencer = sequencer;
        }

        @Override
        public void run() {
            String note;
            while ((note = sequencer.getNextNote(threadNum)) != null) {
                System.out.println(Thread.currentThread().getName() + " is playing: " + note);
                String soundPath = "Sounds/" + note + ".wav";
                filePlayer.play(soundPath);
            }
        }
    }
}