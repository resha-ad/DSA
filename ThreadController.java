//6a

// NumberPrinter class with methods to print 0, even, and odd numbers
class NumberPrinter {
    public void printZero() {
        System.out.print("0");
    }

    public void printEven(int num) {
        System.out.print(num);
    }

    public void printOdd(int num) {
        System.out.print(num);
    }
}

// ThreadController class to coordinate the three threads
class ThreadController {
    private final NumberPrinter printer = new NumberPrinter(); // Instance of NumberPrinter
    private int n; // Maximum number to print up to
    private int current = 1; // Current number to print
    private int turn = 0; // Indicates which thread should print next (0: ZeroThread, 1: OddThread, 2:
                          // EvenThread)

    public ThreadController(int n) {
        this.n = n;
    }

    // Method for ZeroThread
    public void zeroThread() throws InterruptedException {
        synchronized (this) {
            while (current <= n) {
                // Wait until it's ZeroThread's turn
                while (turn != 0) {
                    wait();
                }
                // Print 0 if the sequence is not complete
                if (current <= n) {
                    printer.printZero();
                }
                // Determine the next thread's turn (OddThread or EvenThread)
                turn = (current % 2 == 0) ? 2 : 1;
                notifyAll(); // Notify all waiting threads
            }
        }
    }

    // Method for EvenThread
    public void evenThread() throws InterruptedException {
        synchronized (this) {
            while (current <= n) {
                // Wait until it's EvenThread's turn
                while (turn != 2) {
                    wait();
                }
                // Print the even number if the sequence is not complete
                if (current <= n) {
                    printer.printEven(current);
                    current++; // Move to the next number
                }
                turn = 0; // Set the next turn to ZeroThread
                notifyAll(); // Notify all waiting threads
            }
        }
    }

    // Method for OddThread
    public void oddThread() throws InterruptedException {
        synchronized (this) {
            while (current <= n) {
                // Wait until it's OddThread's turn
                while (turn != 1) {
                    wait();
                }
                // Print the odd number if the sequence is not complete
                if (current <= n) {
                    printer.printOdd(current);
                    current++; // Move to the next number
                }
                turn = 0; // Set the next turn to ZeroThread
                notifyAll(); // Notify all waiting threads
            }
        }
    }
}

// Main class to test the solution
class Main {
    public static void main(String[] args) {
        int n = 5; // Maximum number to print up to
        ThreadController controller = new ThreadController(n);

        // Create ZeroThread
        Thread zeroThread = new Thread(() -> {
            try {
                controller.zeroThread();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        // Create OddThread
        Thread oddThread = new Thread(() -> {
            try {
                controller.oddThread();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        // Create EvenThread
        Thread evenThread = new Thread(() -> {
            try {
                controller.evenThread();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        // Start all threads
        zeroThread.start();
        oddThread.start();
        evenThread.start();

        // Wait for all threads to complete
        try {
            zeroThread.join();
            oddThread.join();
            evenThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}