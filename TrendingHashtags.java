
//4a- Hashmap
import java.util.*;
import java.time.LocalDate;

public class TrendingHashtags {
    // Tweet class to represent a tweet
    static class Tweet {
        int userId;
        int tweetId;
        LocalDate tweetDate;
        String tweet;

        Tweet(int userId, int tweetId, LocalDate tweetDate, String tweet) {
            this.userId = userId;
            this.tweetId = tweetId;
            this.tweetDate = tweetDate;
            this.tweet = tweet;
        }
    }

    // Function to extract hashtags from a tweet
    private static List<String> extractHashtags(String tweet) {
        List<String> hashtags = new ArrayList<>();
        String[] words = tweet.split(" ");
        for (String word : words) {
            if (word.startsWith("#")) {
                hashtags.add(word);
            }
        }
        return hashtags;
    }

    // Function to find the top 3 trending hashtags
    public static List<Map.Entry<String, Integer>> findTopTrendingHashtags(List<Tweet> tweets) {
        // Step 1: Filter tweets from February 2024
        LocalDate startDate = LocalDate.of(2024, 2, 1);
        LocalDate endDate = LocalDate.of(2024, 2, 29);

        // Step 2: Extract hashtags and count their occurrences
        Map<String, Integer> hashtagCounts = new HashMap<>();
        for (Tweet tweet : tweets) {
            if (!tweet.tweetDate.isBefore(startDate) && !tweet.tweetDate.isAfter(endDate)) {
                List<String> hashtags = extractHashtags(tweet.tweet);
                for (String hashtag : hashtags) {
                    hashtagCounts.put(hashtag, hashtagCounts.getOrDefault(hashtag, 0) + 1);
                }
            }
        }

        // Step 3: Sort hashtags by count (descending) and then by hashtag (descending)
        List<Map.Entry<String, Integer>> sortedHashtags = new ArrayList<>(hashtagCounts.entrySet());
        sortedHashtags.sort((a, b) -> {
            int countCompare = b.getValue().compareTo(a.getValue());
            if (countCompare != 0) {
                return countCompare;
            } else {
                return b.getKey().compareTo(a.getKey());
            }
        });

        // Step 4: Select the top 3 hashtags
        return sortedHashtags.subList(0, Math.min(3, sortedHashtags.size()));
    }

    // Main function to test the solution
    public static void main(String[] args) {
        // Sample tweets
        List<Tweet> tweets = Arrays.asList(
                new Tweet(135, 13, LocalDate.of(2024, 2, 1),
                        "Enjoying a great start to the day. #HappyDay #MorningVibes"),
                new Tweet(136, 14, LocalDate.of(2024, 2, 3), "Another #HappyDay with good vibes! #FeelGood"),
                new Tweet(137, 15, LocalDate.of(2024, 2, 4), "Productivity peaks! #Worklife #ProductiveDay"),
                new Tweet(138, 16, LocalDate.of(2024, 2, 4), "Exploring new tech frontiers. #TechLife #Innovation"),
                new Tweet(139, 17, LocalDate.of(2024, 2, 5), "Gratitude for today's moments. #HappyDay #Thankful"),
                new Tweet(140, 18, LocalDate.of(2024, 2, 7), "Innovation drives us. #TechLife #FutureTech"),
                new Tweet(141, 19, LocalDate.of(2024, 2, 9), "Connecting with nature's serenity. #Nature #Peaceful"));

        // Find top 3 trending hashtags
        List<Map.Entry<String, Integer>> topHashtags = findTopTrendingHashtags(tweets);

        // Print the result
        System.out.println("+-----------+-------+");
        System.out.println("| hashtag   | count |");
        System.out.println("+-----------+-------+");
        for (Map.Entry<String, Integer> entry : topHashtags) {
            System.out.printf("| %-10s| %-6d|\n", entry.getKey(), entry.getValue());
        }
        System.out.println("+-----------+-------+");
    }
}