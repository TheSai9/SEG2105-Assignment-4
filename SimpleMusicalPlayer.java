/**
 * Simple MusicPlayer implementation for SEG2105 Assignment #4
 *
 * This implementation uses a shared sequencer object to coordinate
 * between two threads playing musical notes.
 *
 * Task 1:
 *  - Thread 1 plays: do, mi, sol, si, do-octave
 *  - Thread 2 plays: re, fa, la, do-octave
 *  - The user hears: do, re, mi, fa, sol, la, si, do-octave
 *  - The final do-octave is played by BOTH threads at the same time.
 *
 * Task 2:
 *  - Twinkle Twinkle Little Star, with note restrictions:
 *    - Thread 1: do, mi, sol, si, do-octave
 *    - Thread 2: re, fa, la, do-octave
 *
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
     * Thread 1: do, mi, sol, si, do-octave
     * Thread 2: re, fa, la, do-octave
     *
     * Heard sequence: do, re, mi, fa, sol, la, si, do-octave
     * Final do-octave is played simultaneously by both threads.
     */
    public void playScale() {
        // We define the overall "heard" notes:
        String[] notes = {"do", "re", "mi", "fa", "sol", "la", "si", "do-octave"};
        int[] threadIds = {1, 2, 1, 2, 1, 2, 1, 0}; 
        // For the last index, threadIds = 0 means "both threads will play do-octave"

        sequencer.setSequence(notes, threadIds);
        playWithThreads();
    }

    /**
     * Task 2: Play Twinkle Twinkle Little Star
     * Line 1: do do sol sol la la sol fa fa mi mi re re do
     * Line 2: sol sol fa fa mi mi re sol sol fa fa mi mi re
     * Line 3: do do sol sol la la sol fa fa mi mi re re do
     *
     * Thread 1: do, mi, sol, si, do-octave
     * Thread 2: re, fa, la, do-octave
     */
    public void playTwinkleTwinkleLittleStar() {
        // Line 1: do do sol sol la la sol fa fa mi mi re re do
        String[] line1_notes = {"do", "do", "sol", "sol", "la", "la", "sol", "fa", "fa", "mi", "mi", "re", "re", "do"};
        int[] line1_threads = {1, 1, 1, 1, 2, 2, 1, 2, 2, 1, 1, 2, 2, 1};

        // Line 2: sol sol fa fa mi mi re sol sol fa fa mi mi re
        String[] line2_notes = {"sol", "sol", "fa", "fa", "mi", "mi", "re", "sol", "sol", "fa", "fa", "mi", "mi", "re"};
        int[] line2_threads = {1, 1, 2, 2, 1, 1, 2, 1, 1, 2, 2, 1, 1, 2};

        // Line 3: do do sol sol la la sol fa fa mi mi re re do
        String[] line3_notes = {"do", "do", "sol", "sol", "la", "la", "sol", "fa", "fa", "mi", "mi", "re", "re", "do"};
        int[] line3_threads = {1, 1, 1, 1, 2, 2, 1, 2, 2, 1, 1, 2, 2, 1};

        // Combine all lines
        String[] allNotes = new String[line1_notes.length + line2_notes.length + line3_notes.length];
        int[] allThreads = new int[line1_threads.length + line2_threads.length + line3_threads.length];

        int idx = 0;
        for (int i = 0; i < line1_notes.length; i++) {
            allNotes[idx] = line1_notes[i];
            allThreads[idx] = line1_threads[i];
            idx++;
        }
        for (int i = 0; i < line2_notes.length; i++) {
            allNotes[idx] = line2_notes[i];
            allThreads[idx] = line2_threads[i];
            idx++;
        }
        for (int i = 0; i < line3_notes.length; i++) {
            allNotes[idx] = line3_notes[i];
            allThreads[idx] = line3_threads[i];
            idx++;
        }

        sequencer.setSequence(allNotes, allThreads);
        playWithThreads();
    }

    /**
     * Create and start the two player threads
     */
    private void playWithThreads() {
        Thread thread1 = new Thread(new PlayerThread(1, filePlayer, sequencer), "Thread-1");
        Thread thread2 = new Thread(new PlayerThread(2, filePlayer, sequencer), "Thread-2");

        thread1.start();
        thread2.start();

        // Wait for both threads to finish
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

        try {
            Thread.sleep(2000); // Pause between songs
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("\n=== Playing Twinkle Twinkle Little Star (Task 2) ===");
        player = new SimpleMusicalPlayer();
        player.playTwinkleTwinkleLittleStar();
    }

    /**
     * Inner class: Manages the sequence of notes with proper turn-based coordination.
     * Also handles the special "both threads play do-octave" case for Task 1.
     */
    private static class Sequencer {
        private String[] notes;
        private int[] threadIds;
        private int currentIndex;
        private final Object lock = new Object();

        private static final long WAIT_TIMEOUT = 2000; // 2 seconds
        private static final long NOTE_DELAY = 600;    // global delay between notes

        // For the simultaneous do-octave:
        private boolean bothPlayFlag = false;      // tells both threads to play do-octave
        private boolean bothPlayedDone = false;    // set to true when both have finished playing it
        private int bothPlayCount = 0;             // how many threads have taken the special note

        public void setSequence(String[] notes, int[] threadIds) {
            this.notes = notes;
            this.threadIds = threadIds;
            this.currentIndex = 0;
            this.bothPlayFlag = false;
            this.bothPlayedDone = false;
            this.bothPlayCount = 0;
        }

        /**
         * Get the next note that should be played by the given thread.
         * - Normal case: threadIds[currentIndex] == threadNum → that thread plays notes[currentIndex]
         * - Special Task 1 case: threadIds[currentIndex] == 0 and note is "do-octave"
         *   → both threads play the same note once, at the same time.
         */
        public String getNextNote(int threadNum) {
            synchronized (lock) {
                // If we've already finished completely
                if (currentIndex >= notes.length && !bothPlayFlag) {
                    return null;
                }

                // Special simultaneous section already handled and done
                if (bothPlayedDone) {
                    return null;
                }

                long startTime = System.currentTimeMillis();

                while (true) {
                    // If we are in the special both-play-do-octave phase
                    if (bothPlayFlag) {
                        if (bothPlayCount < 2) {
                            // This thread can participate in playing do-octave
                            bothPlayCount++;

                            // Once both have taken it, mark done
                            if (bothPlayCount == 2) {
                                bothPlayedDone = true;
                            }

                            // No index increment here, the note is the same
                            lock.notifyAll();
                            return "do-octave";
                        } else {
                            // Both threads already got it
                            return null;
                        }
                    }

                    // If we've reached or passed the end without entering special mode
                    if (currentIndex >= notes.length) {
                        return null;
                    }

                    // Normal case: check whose turn it is
                    int assignedThread = threadIds[currentIndex];

                    // Special value 0 means: both threads will play this note (do-octave) at once
                    if (assignedThread == 0 && "do-octave".equals(notes[currentIndex])) {
                        // Enter special both-play mode
                        bothPlayFlag = true;
                        bothPlayCount = 0;
                        // Do not advance currentIndex; both threads will use this same note
                        // Wait for this thread's turn to grab it via the bothPlayFlag branch
                        continue;
                    }

                    // Normal single-thread case
                    if (assignedThread == threadNum) {
                        String note = notes[currentIndex];
                        currentIndex++;

                        // Delay before allowing the next note to be chosen
                        try {
                            Thread.sleep(NOTE_DELAY);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return null;
                        }

                        lock.notifyAll();
                        return note;
                    }

                    // Not this thread's turn yet, wait a bit
                    try {
                        lock.wait(50);
                        if (System.currentTimeMillis() - startTime > WAIT_TIMEOUT) {
                            return null;
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }
            }
        }
    }

    /**
     * Inner class: Represents a player thread that plays assigned notes
     */
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
                // Timing is handled by the Sequencer.
            }
        }
    }
}
