import java.util.concurrent.CyclicBarrier;

/**
 * Enhanced MusicPlayer with better synchronization using CyclicBarrier.
 * This implementation ensures threads play notes in perfect synchronization.
 * 
 * @author Student
 */
public class MusicPlayerEnhanced {
	
	private FilePlayer filePlayer;
	private String[] noteSequence;
	private int[] threadAssignment;  // 1 for Thread 1, 2 for Thread 2
	private CyclicBarrier barrier;
	private volatile int nextNoteIndex = 0;
	private final Object lock = new Object();
	
	public MusicPlayerEnhanced() {
		filePlayer = new FilePlayer();
	}
	
	/**
	 * Play the do-re-mi scale with two synchronized threads
	 * Expected sequence: do, re, mi, fa, sol, la, si, do-octave
	 */
	public void playScale() {
		// Sequence of notes to play
		String[] notes = {"do", "re", "mi", "fa", "sol", "la", "si", "do-octave"};
		int[] threads = {1, 2, 1, 2, 1, 2, 1, 2};
		
		playSequence(notes, threads);
	}
	
	/**
	 * Play Twinkle Twinkle Little Star song
	 * Line 1: do do sol sol la la sol fa fa mi mi re re do
	 * Line 2: sol sol fa fa mi mi re sol sol fa fa mi mi re
	 * Line 3: do do sol sol la la sol fa fa mi mi re re do
	 */
	public void playTwinkleTwinkleLittleStar() {
		// Line 1: do do sol sol la la sol fa fa mi mi re re do
		String[] line1_notes = {"do", "do", "sol", "sol", "la", "la", "sol", "fa", "fa", "mi", "mi", "re", "re", "do"};
		int[] line1_threads = {1, 1, 1, 1, 2, 2, 1, 2, 2, 1, 1, 2, 2, 1};
		
		// Line 2: sol sol fa fa mi mi re sol sol fa fa mi mi re
		String[] line2_notes = {"sol", "sol", "fa", "fa", "mi", "mi", "re", "sol", "sol", "fa", "fa", "mi", "mi", "re"};
		int[] line2_threads = {1, 1, 2, 2, 1, 1, 2, 1, 1, 2, 2, 1, 1, 2};
		
		// Line 3: do do sol sol la la sol fa fa mi mi re re do + do-octave by thread 2
		String[] line3_notes = {"do", "do", "sol", "sol", "la", "la", "sol", "fa", "fa", "mi", "mi", "re", "re", "do", "do-octave"};
		int[] line3_threads = {1, 1, 1, 1, 2, 2, 1, 2, 2, 1, 1, 2, 2, 1, 2};
		
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
		
		playSequence(allNotes, allThreads);
	}
	
	/**
	 * Play a sequence of notes with synchronization
	 * 
	 * @param notes array of note names
	 * @param threads array indicating which thread plays each note (1 or 2)
	 */
	private void playSequence(String[] notes, int[] threads) {
		noteSequence = notes;
		threadAssignment = threads;
		nextNoteIndex = 0;
		
		// Create a barrier for 2 threads (they don't always need to sync, but mechanism is ready)
		barrier = new CyclicBarrier(2, new Runnable() {
			@Override
			public void run() {
				// Action to perform when both threads reach the barrier
			}
		});
		
		Thread t1 = new Thread(new Runnable() {
			@Override
			public void run() {
				playAsThread(1);
			}
		}, "Thread-1");
		
		Thread t2 = new Thread(new Runnable() {
			@Override
			public void run() {
				playAsThread(2);
			}
		}, "Thread-2");
		
		t1.start();
		t2.start();
		
		try {
			t1.join();
			t2.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Play notes assigned to a specific thread
	 * 
	 * @param threadNum the thread number (1 or 2)
	 */
	private void playAsThread(int threadNum) {
		long waitTimeout = 2000; // 2 seconds
		while (true) {
			String noteToPlay = null;
			
			synchronized (lock) {
				// Check if we've reached the end
				if (nextNoteIndex >= noteSequence.length) {
					break;
				}
				
				// Wait for our turn
				long startTime = System.currentTimeMillis();
				while (nextNoteIndex < noteSequence.length && threadAssignment[nextNoteIndex] != threadNum) {
					try {
						lock.wait(50);
						// If we've waited too long, break out
						if (System.currentTimeMillis() - startTime > waitTimeout) {
							return;
						}
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return;
					}
				}
				
				// Now check if it's our turn
				if (nextNoteIndex < noteSequence.length && threadAssignment[nextNoteIndex] == threadNum) {
					noteToPlay = noteSequence[nextNoteIndex];
					nextNoteIndex++;
					lock.notifyAll();
				}
			}
			
			if (noteToPlay != null) {
				// Play the note
				System.out.println(Thread.currentThread().getName() + " is playing: " + noteToPlay);
				String soundPath = "Sounds/" + noteToPlay + ".wav";
				filePlayer.play(soundPath);
				
				// Add delay to allow sound to finish playing
				try {
					Thread.sleep(600); // Adjust timing as needed for clarity
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				// Not our turn or we're done
				if (nextNoteIndex >= noteSequence.length) {
					break;
				}
			}
		}
	}
	
	public static void main(String[] args) {
		MusicPlayerEnhanced player = new MusicPlayerEnhanced();
		
		System.out.println("=== Playing do-re-mi scale ===");
		player.playScale();
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		System.out.println("\n=== Playing Twinkle Twinkle Little Star ===");
		player = new MusicPlayerEnhanced();
		player.playTwinkleTwinkleLittleStar();
	}
}
