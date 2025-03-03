//Question 2a (greedy approach)
public class MinimumRewards {

    public static int minRewards(int[] ratings) {
        int n = ratings.length;
        if (n == 0)
            return 0;

        int[] rewards = new int[n];
        // Every employee gets at least one reward initially
        for (int i = 0; i < n; i++) {
            rewards[i] = 1;
        }

        // Left-to-right pass
        for (int i = 1; i < n; i++) {
            if (ratings[i] > ratings[i - 1]) {
                rewards[i] = rewards[i - 1] + 1;
            }
        }

        // Right-to-left pass
        for (int i = n - 2; i >= 0; i--) {
            if (ratings[i] > ratings[i + 1]) {
                rewards[i] = Math.max(rewards[i], rewards[i + 1] + 1);
            }
        }

        // Sum up the total rewards
        int minRewards = 0;
        for (int reward : rewards) {
            minRewards += reward;
        }

        return minRewards;
    }

    public static void main(String[] args) {
        // Test Case 1
        int[] ratings1 = { 1, 0, 2 };
        System.out.println("Test Case 1:");
        System.out.println("Input: ratings = [1, 0, 2]");
        System.out.println("Output: " + minRewards(ratings1)); // Expected: 5

        // Test Case 2
        int[] ratings2 = { 1, 2, 2 };
        System.out.println("\nTest Case 2:");
        System.out.println("Input: ratings = [1, 2, 2]");
        System.out.println("Output: " + minRewards(ratings2)); // Expected: 4

        // Test Case 3
        int[] ratings3 = { 4, 3, 2, 1, 2, 3, 4 };
        System.out.println("\nTest Case 3:");
        System.out.println("Input: ratings = [4, 3, 2, 1, 2, 3, 4]");
        System.out.println("Output: " + minRewards(ratings3)); // Expected: 19
    }
}