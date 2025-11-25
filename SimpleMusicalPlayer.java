/**
 * Simple MusicPlayer implementation for SEG2105 Assignment #4
 * 
 * This implementation uses a shared sequencer object to coordinate
 * between two threads playing musical notes.
 * 
 * @author Student
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
	 * Expected output sequence: do, re, mi, fa, sol, la, si, do-octave
	 */
	public void playScale() {
		// Define the complete sequence with thread assignments
		String[] notes = {"do", "re", "mi", "fa", "sol", "la", "si", "do-octave"};
		int[] threadIds = {1, 2, 1, 2, 1, 2, 1, 2};
		
		sequencer.setSequence(notes, threadIds);
		playWithThreads();
	}
	
	/**
	 * Task 2: Play Twinkle Twinkle Little Star
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
		
		System.out.println("=== Playing do-re-mi scale ===");
		player.playScale();
		
		try {
			Thread.sleep(2000); // Pause between songs
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		System.out.println("\n=== Playing Twinkle Twinkle Little Star ===");
		player = new SimpleMusicalPlayer();
		player.playTwinkleTwinkleLittleStar();
	}
	
	/**
	 * Inner class: Manages the sequence of notes with proper turn-based coordination
	 */
	private static class Sequencer {
		private String[] notes;
		private int[] threadIds;
		private int currentIndex;
		private final Object lock = new Object();
		private static final long WAIT_TIMEOUT = 2000; // 2 seconds - allow time for audio playback
		
		public void setSequence(String[] notes, int[] threadIds) {
			this.notes = notes;
			this.threadIds = threadIds;
			this.currentIndex = 0;
		}
		
		/**
		 * Get the next note that should be played by the given thread
		 * Uses turn-based coordination to ensure sequential play
		 */
		public String getNextNote(int threadNum) {
			synchronized (lock) {
				// Wait for our turn (with timeout to detect finished state)
				long startTime = System.currentTimeMillis();
				while (currentIndex < notes.length && threadIds[currentIndex] != threadNum) {
					try {
						lock.wait(50); // Wait and check periodically
						// If we've waited too long and still not our turn, break
						if (System.currentTimeMillis() - startTime > WAIT_TIMEOUT) {
							return null; // Timeout waiting for our turn
						}
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return null;
					}
				}
				
				// Check if we've reached the end
				if (currentIndex >= notes.length) {
					return null;
				}
				
				// Get and play the note
				String note = notes[currentIndex];
				currentIndex++;
				
				// Notify waiting threads
				lock.notifyAll();
				
				return note;
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
				
				// Wait for the sound to finish playing
				try {
					Thread.sleep(600); // Adjust based on audio file length
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
