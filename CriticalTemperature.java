//Question 1a (divide-and-conquer strategy combined with dynamic programming)
public class CriticalTemperature {

    // Function to find the minimum number of measurements
    public static int findMinMeasurements(int k, int n) {
        // DP table to store results of subproblems
        int[][] dp = new int[k + 1][n + 1];

        // Base cases:
        // If there is only 1 sample, we need to check all temperatures sequentially
        for (int i = 1; i <= n; i++) {
            dp[1][i] = i;
        }

        // If there are 0 temperatures, no measurements are needed
        for (int i = 1; i <= k; i++) {
            dp[i][0] = 0;
        }

        // Fill the DP table
        for (int i = 2; i <= k; i++) { // Number of samples
            for (int j = 1; j <= n; j++) { // Number of temperature levels
                dp[i][j] = Integer.MAX_VALUE;
                for (int x = 1; x <= j; x++) {
                    // If the material reacts, we lose one sample and check below x
                    // If it doesn't react, we retain the sample and check above x
                    int res = 1 + Math.max(dp[i - 1][x - 1], dp[i][j - x]);
                    dp[i][j] = Math.min(dp[i][j], res);
                }
            }
        }

        return dp[k][n];
    }

    // Main function to test the implementation
    public static void main(String[] args) {
        int[][] testCases = {
                { 1, 2, 2 }, // Example 1
                { 2, 6, 3 }, // Example 2
                { 3, 14, 4 }, // Example 3
        };

        // Run all test cases
        for (int[] testCase : testCases) {
            int k = testCase[0];
            int n = testCase[1];
            int expected = testCase[2];
            int result = findMinMeasurements(k, n);

            // Print test case details
            System.out.println("Test Case: k = " + k + ", n = " + n);
            System.out.println("Expected Output: " + expected);
            System.out.println("Actual Output: " + result);
            System.out.println("Result: " + (result == expected ? "PASS" : "FAIL"));
            System.out.println();
        }
    }
}