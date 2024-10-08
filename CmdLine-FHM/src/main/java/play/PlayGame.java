package play;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import constant.WheelConstant;
import constant.WordConstant;
import model.*;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.util.*;

public class PlayGame {

    private static final Scanner scan = new Scanner(System.in);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final Gson gson = new Gson();
    // support async or sync connection
    final static CloseableHttpClient httpClient = HttpClients.createDefault();

    private static Game game = new Game();
    private static List<Team> teamList = new ArrayList<>();
    private static Team team = new Team();
    private static List<RoundsInGame> roundList = new ArrayList<>();
    private static final Round round = new Round();
    private static final List<Integer> wordIdList = new ArrayList<>();
    private static Word word = new Word();

    private static StringBuilder controlWord = new StringBuilder();
    private static StringBuilder wordToGuess = new StringBuilder();
    private static Map<Character, List<Integer>> charInWordMap = new HashMap<>();
    private static Set<Character> charGuessedSet = new HashSet<>();

    private static List<String> wheel = WheelConstant.wheel();
    private static List<String> noneNumWheelResult = WheelConstant.noneNumWheelResult();
    private static final Set<Character> symbolsNoSpaceAfter = WordConstant.symbolsNoSpaceAfter();
    private static final Set<Character> symbolsWithSpaceAfter = WordConstant.symbolsWithSpaceAfter();
    private static final Set<Character> vowels = WordConstant.vowel();

    public static void addNewGame() throws IOException, URISyntaxException {

        // check game name input
        while (true) {

            System.out.println();
            System.out.print("Game Name: ");
            String gameName = scan.nextLine();

            if (!gameName.trim().isEmpty()) {

                game.setGameName(gameName);
                break;
            } else {

                System.out.println("Game name cannot be blank.");
            }
        }

        System.out.println();
        System.out.print("Total Number of Contestants: ");
        int totalTeam;

        while (true) {
            try {
                totalTeam = Integer.parseInt(scan.nextLine());

                if (totalTeam < 2 || totalTeam > 4) { // if total team is not btw 2-4
//                    System.out.println("int");
                    System.out.println("Total number of contestants must be a whole number and must be between 2-4.");
                    System.out.println();
                    System.out.print("Total Number of Contestants: ");

                } else { // pass validation

                    game.setTotalTeam(totalTeam);
                    break;
                }
            } catch (NumberFormatException e) { // if total team is not int
//                System.out.println("not int");
                System.out.println("Total number of contestants must be a whole number and must be between 2-4.");
                System.out.println();
                System.out.print("Total Number of Contestants: ");
            }
        }

    /*
        Request
     */
        HttpPost addGameRequest = new HttpPost("http://localhost:8080/game");
        StringEntity gameToAddEntity = new StringEntity(mapper.writeValueAsString(game)
                , ContentType.APPLICATION_JSON);
        addGameRequest.setEntity(gameToAddEntity);

    /*
        Response
     */
        CloseableHttpResponse addedGameResponse = httpClient.execute(addGameRequest);

        if (addedGameResponse.getStatusLine().

                getStatusCode() != 200) {
            System.out.println("Game is not added! Status code: " + addedGameResponse.getStatusLine().getStatusCode() + " - " + addedGameResponse.getStatusLine().getReasonPhrase());
        }

        HttpEntity responseEntity = addedGameResponse.getEntity();
        game = mapper.readValue(EntityUtils.toString(responseEntity), new TypeReference<Game>() {
        });

        addContestant();

    }

    public static void addContestant() throws IOException, URISyntaxException {

        int totalTeam = game.getTotalTeam();
        String teamName;
        int teamTurn;
        int gameId = game.getGameId();
        List<Integer> teamIds = new ArrayList<>();

        for (int i = 1; i < totalTeam + 1; i++) {

            // validate team name input
            while (true) {

                System.out.println();
                System.out.print("Contestant " + i + " Name: ");
                teamName = scan.nextLine();

                if (!teamName.trim().isEmpty()) {
                    break;
                } else {

                    System.out.println("Contestant name cannot be blank.");
                }
            }

            teamTurn = i; // calculate team turn

            Team teamToAdd = new Team();
            teamToAdd.setTeamName(teamName);
            teamToAdd.setTeamTurn(teamTurn);
            teamToAdd.setGameId(gameId);

            HttpPost addTeamRequest = new HttpPost("http://localhost:8080/team");
            StringEntity teamToAddEntity = new StringEntity(mapper.writeValueAsString(teamToAdd)
                    , ContentType.APPLICATION_JSON);
            addTeamRequest.setEntity(teamToAddEntity);

            CloseableHttpResponse addedTeamResponse = httpClient.execute(addTeamRequest);

            if (addedTeamResponse.getStatusLine().getStatusCode() != 200) {
                System.out.println("Round is not added! Status code: " + addedTeamResponse.getStatusLine().getStatusCode() + " - " + addedTeamResponse.getStatusLine().getReasonPhrase());
            }

            HttpEntity responseEntity = addedTeamResponse.getEntity();
            Team teamAdded = mapper.readValue(EntityUtils.toString(responseEntity)
                    , new TypeReference<Team>() {
                    });

            teamList.add(teamAdded);

            teamAdded.getTeamId();
//            System.out.println("teamAdded.getTeamId(): " + teamAdded.getTeamId());
            teamIds.add(teamAdded.getTeamId());
//            System.out.println("teamIds: " + teamIds);
        }

        addRound(teamIds);
    }

    public static void addRound(List<Integer> teamIds) throws IOException, URISyntaxException {

//        game.setGameId(3);

        AddRound roundToAdd = new AddRound();

        roundToAdd.setTeamIds(teamIds);
        roundToAdd.setGameId(game.getGameId());

        HttpPost addRoundRequest = new HttpPost("http://localhost:8080/round");
        StringEntity roundToAddEntity = new StringEntity(mapper.writeValueAsString(roundToAdd)
                , ContentType.APPLICATION_JSON);

        addRoundRequest.setEntity(roundToAddEntity);
//        System.out.println("addRoundRequest: " + EntityUtils.toString(roundToAddEntity));

        CloseableHttpResponse addedRoundResponse = httpClient.execute(addRoundRequest);

        if (addedRoundResponse.getStatusLine().getStatusCode() != 200) {
            System.out.println("Team is not added! Status code: " + addedRoundResponse.getStatusLine().getStatusCode() + " - " + addedRoundResponse.getStatusLine().getReasonPhrase());
        }
        HttpEntity responseEntity = addedRoundResponse.getEntity();
        roundList = mapper.readValue(EntityUtils.toString(responseEntity)
                , new TypeReference<>() {
                });

        setRound();
    }

    public static void setRound() throws IOException, URISyntaxException {

//        System.out.println("roundList: " + roundList);

        for (int i = 0; i < roundList.size(); i++) {

            resetWordToGuessInfo();

//            System.out.println(charInWordMap.toString());
//            System.out.println(controlWord);
//            System.out.println(controlWord.length() - 1);
//            System.out.println(wordToGuess);
//            System.out.println(wordToGuess.length() - 1);

            System.out.println();
            System.out.println("=====");
            System.out.println("Round " + (i + 1) + " begin!!!");
            System.out.println("=====");

            int teamCounter = i;

            // set round
            round.setRoundId(roundList.get(i).getRoundId().getRoundId());
            round.setTeamId(roundList.get(i).getRoundId().getTeamId());
            round.setGameId(roundList.get(i).getRoundId().getGameId());

            getTeamByTeamId(round.getTeamId()); // set team according to teamId in round

            System.out.println();
            System.out.println("Contestant " + team.getTeamTurn() + " turn.");

            printContestantInformation();

            // set game
            chooseCategory();
            processChosenWord();
            updateGameDb(); // does not update currentRound in game DB
            updateCurrentRoundInGameDb(i + 1); // update currentRound in game DB

//            System.out.println(charInWordMap.toString());
//            System.out.println(controlWord);
//            System.out.println(controlWord.length() - 1);
//            System.out.println(wordToGuess);
//            System.out.println(wordToGuess.length() - 1);

            // play the game
            playTheGame(teamCounter); // play the round

            System.out.println("");
            System.out.println("=====");
            System.out.println("All contestants information:");
            updateTotalScoresResetRoundScoresSpinScores(); // print each team info as well
        }

        System.out.println();
        System.out.println("GAME OVER!");
        System.out.println("Let see who is the winner...");

        getTeamsByGameId();
        calculateWinner();
    }

    private static void resetWordToGuessInfo() {
        wordToGuess.setLength(0);
        controlWord.setLength(0);
        charGuessedSet.clear();
        charInWordMap.clear();
    }

    private static void updateTotalScoresResetRoundScoresSpinScores() throws IOException, URISyntaxException {

        for (Team teamInList : teamList) {

            getTeamByTeamId(teamInList.getTeamId());
            getRoundByRoundIdTeamId(round.getRoundId(), team.getTeamId());

            // update team total score
            team.setTotalScore(team.getTotalScore() + round.getRoundScore());
            updateTeamDb(team.getTeamId());

            // reset roundScore spinScore
            round.setRoundScore(0);
            round.setSpinScore(0);
            round.setSpinToken(false);
            updateRoundDb();

            printContestantInformation();
        }
    }

    private static void calculateWinner() {

        String winningTeamName = null;
        int highestScore = 0;

        for (Team team : teamList) {

            System.out.println();
            System.out.println(team.getTeamName() + " have " + team.getTotalScore() + " points.");
            System.out.println("=====");

            if (team.getTotalScore() > highestScore) {
                highestScore = team.getTotalScore();
                winningTeamName = team.getTeamName();
            }
        }

        System.out.println();
        System.out.println("===============================");
        System.out.println(" ________         $$$$$");
        System.out.println(" |      |        $  $  $");
        System.out.println(" $      |       $   $");
        System.out.println("/|\\     |        $  $");
        System.out.println(" |      |         $$$$$");
        System.out.println("/ \\     |           $  $");
        System.out.println("        |           $   $");
        System.out.println("        |        $  $  $");
        System.out.println("     -------      $$$$$");
        System.out.println("  =====================");
        System.out.println(" |  Fortune Hang Man!  |");
        System.out.println("  =====================");
        System.out.println();
        System.out.println("The winner is.... ");
        System.out.println();
        System.out.println("`¤´.¸¸.·´¨»*«| " + winningTeamName + " |»*«´¨`·.¸¸.`¤´");
    }

    public static void getTeamsByGameId() throws IOException {

//        game.setGameId(12);

        HttpGet getTeamsByGameIdRequest = new HttpGet("http://localhost:8080/team/game/" + game.getGameId());

        CloseableHttpResponse getTeamByGameIdResponse = httpClient.execute(getTeamsByGameIdRequest);
        HttpEntity responseEntity = getTeamByGameIdResponse.getEntity();

        if (getTeamByGameIdResponse.getStatusLine().getStatusCode() != 200) {
            System.out.println("Current round in game is not updated! Status code: " + getTeamByGameIdResponse.getStatusLine().getStatusCode() + " - " + getTeamByGameIdResponse.getStatusLine().getReasonPhrase());
        }

        teamList = mapper.readValue(EntityUtils.toString(responseEntity), new TypeReference<List<Team>>() {
        });

//        for (Team team : teamList) {
//            System.out.println(team.toString());
//        }
    }

    public static void updateCurrentRoundInGameDb(int currentRound) throws IOException, URISyntaxException {

//        game.setGameId(2);

        URIBuilder builder = new URIBuilder("http://localhost:8080/game/" + game.getGameId());

        if (game.getCurrentRound() != currentRound) {
//            System.out.println("updatingGame currentRound: currentRound = " + currentRound);
            builder.setParameter("currentRound", Integer.toString(currentRound));
        }

        HttpPut updateGameRequest = new HttpPut(builder.build());
        StringEntity updateGameEntity = new StringEntity(mapper.writeValueAsString(updateGameRequest), ContentType.APPLICATION_JSON);
        updateGameRequest.setEntity(updateGameEntity);

//        System.out.println(updateGameRequest.toString());

        CloseableHttpResponse updateGameResponse = httpClient.execute(updateGameRequest);

        if (updateGameResponse.getStatusLine().getStatusCode() != 200) {
            System.out.println("Current round in game is not updated! Status code: " + updateGameResponse.getStatusLine().getStatusCode() + " - " + updateGameResponse.getStatusLine().getReasonPhrase());
        }

        HttpEntity responseEntity = updateGameResponse.getEntity();
        game = mapper.readValue(EntityUtils.toString(responseEntity), new TypeReference<Game>() {
        });

//        System.out.println("Game: " + game.toString());
    }

    public static void chooseCategory() throws IOException, URISyntaxException {
        HttpGet getCategoriesRequest = new HttpGet("http://localhost:8080/category");

        CloseableHttpResponse getCategoriesResponse = httpClient.execute(getCategoriesRequest);
        HttpEntity getCategoriesEntity = getCategoriesResponse.getEntity();

        if (getCategoriesResponse.getStatusLine().getStatusCode() != 200) {
            System.out.println("Cannot get categories! Status code: " + getCategoriesResponse.getStatusLine().getStatusCode() + " - " + getCategoriesResponse.getStatusLine().getReasonPhrase());
        }

        List<String> categories = mapper.readValue(EntityUtils.toString(getCategoriesEntity), new TypeReference<>() {
        });
        StringBuilder categoriesList = new StringBuilder();

        for (int i = 0; i < categories.size(); i++) {
            if (i != categories.size() - 1) {
                categoriesList.append(categories.get(i) + ", ");
            } else {
                categoriesList.append(categories.get(i));
            }
        }

        // choose category
        System.out.println();
        System.out.println("=====");
        System.out.println("Choose one of these categories: " + categoriesList);
        System.out.print("Category: ");
        String categoryName;

        while (true) {
            try {
                categoryName = scan.nextLine().toUpperCase();

                if (!categories.contains(categoryName.toUpperCase())) {
                    System.out.println();
                    System.out.println("Category chosen is not in the list. Please type one of these categories: " + categoriesList);
                    System.out.print("Category: ");
                } else {
                    categoryName = categoryName.replaceAll(" ", "%20");

                    // get random word
                    getRandomWord(categoryName);
//                    System.out.println("word before: " + word);
                    break;
                }
            } catch (NumberFormatException e) {
                System.out.println("Category chosen is not in the list. Please type one of these categories:" + categoriesList);
                System.out.print("Category: ");
            }
        }

        if (wordIdList.contains(word.getWordId())) {
            getRandomWord(categoryName);
        } else {
            wordIdList.add(word.getWordId());
        }

//        System.out.println("word: " + word);
//        System.out.println("wordIdList: " + wordIdList);
    }

    private static void getRandomWord(String categoryName) throws IOException, URISyntaxException {
        HttpGet getRandomWordRequest = new HttpGet("http://localhost:8080/randWord/" + categoryName);

        CloseableHttpResponse getRandomWordReqponse = httpClient.execute(getRandomWordRequest);
        HttpEntity getRandomWordEntity = getRandomWordReqponse.getEntity();

        if (getRandomWordReqponse.getStatusLine().getStatusCode() != 200) {
            System.out.println("Cannot get random word! Status code: " + getRandomWordReqponse.getStatusLine().getStatusCode() + " - " + getRandomWordReqponse.getStatusLine().getReasonPhrase());
        }

        word = mapper.readValue(EntityUtils.toString(getRandomWordEntity), new TypeReference<Word>() {
        });

        updateWordInGameDb();
    }

    public static void updateWordInGameDb() throws URISyntaxException, IOException {

//        game.setGameId(2);
//        word.setWordId(1);

        URIBuilder builder = new URIBuilder("http://localhost:8080/game/" + game.getGameId());

        if (game.getWord() == null || (word.getWordId() != game.getWord().getWordId())) {
//            System.out.println("updatingGame Word: wordId = " + word.getWordId());
            builder.setParameter("wordId", Integer.toString(word.getWordId()));
            game.setWord(word);
        }

        HttpPut updateGameRequest = new HttpPut(builder.build());
        StringEntity updateGameEntity = new StringEntity(mapper.writeValueAsString(updateGameRequest), ContentType.APPLICATION_JSON);
        updateGameRequest.setEntity(updateGameEntity);

//        System.out.println(updateGameRequest.toString());

        CloseableHttpResponse updateGameResponse = httpClient.execute(updateGameRequest);

        if (updateGameResponse.getStatusLine().getStatusCode() != 200) {
            System.out.println("Word in game is not updated! Status code: " + updateGameResponse.getStatusLine().getStatusCode() + " - " + updateGameResponse.getStatusLine().getReasonPhrase());
        }

        HttpEntity responseEntity = updateGameResponse.getEntity();
        game = mapper.readValue(EntityUtils.toString(responseEntity), new TypeReference<Game>() {
        });

//        System.out.println("Game: " + game.toString());
    }

    public static void updateGameDb() throws URISyntaxException, IOException {

//        game.setGameId(1);
//        round.setRoundId(4);
//        game.setRoundId(3);

        URIBuilder builder = new URIBuilder("http://localhost:8080/game/" + game.getGameId());

        if (round.getRoundId() != game.getRoundId()) {
//            System.out.println("updatingGame Round: roundId = " + round.getRoundId() + " gameId = " + game.getRoundId());
            builder.setParameter("roundId", Long.toString(round.getRoundId()));
            game.setRoundId(round.getRoundId());
        }

        if (game.getGameStatus() == null) {
//            System.out.println("updating game status");
            builder.addParameter("gameStatus", "in progress");
            game.setGameStatus("IN PROGRESS");
        }

        if (team.getTeamTurn() != game.getCurrentTeamTurn()) {
//            System.out.println("updatingGame currentTeamTurn: teamTeamTurn = " + team.getTeamTurn() + " game.currentTeamTurn = " + game.getCurrentTeamTurn());
            builder.addParameter("currentTeamTurn", Integer.toString(team.getTeamTurn()));
            game.setCurrentTeamTurn(team.getTeamTurn());
        }

        HttpPut updateGameRequest = new HttpPut(builder.build());
        StringEntity updateGameEntity = new StringEntity(mapper.writeValueAsString(updateGameRequest), ContentType.APPLICATION_JSON);
        updateGameRequest.setEntity(updateGameEntity);

//        System.out.println(updateGameRequest.toString());

        CloseableHttpResponse updateGameResponse = httpClient.execute(updateGameRequest);

        if (updateGameResponse.getStatusLine().getStatusCode() != 200) {
            System.out.println("Game is not updated! Status code: " + updateGameResponse.getStatusLine().getStatusCode() + " - " + updateGameResponse.getStatusLine().getReasonPhrase());
        }

        HttpEntity responseEntity = updateGameResponse.getEntity();
        game = mapper.readValue(EntityUtils.toString(responseEntity), new TypeReference<Game>() {
        });

//        System.out.println("Game: " + game.toString());
    }

    private static void updateCharGuessedInGameDb(char charGuessed) throws URISyntaxException, IOException {

        URIBuilder builder = new URIBuilder("http://localhost:8080/game/" + game.getGameId());

        String charGuessedString = Character.toString(charGuessed);
        builder.addParameter("letterGuessed", charGuessedString);
        game.setLetterGuessed(charGuessedString);

        HttpPut updateGameRequest = new HttpPut(builder.build());
        StringEntity updateGameEntity = new StringEntity(mapper.writeValueAsString(updateGameRequest), ContentType.APPLICATION_JSON);
        updateGameRequest.setEntity(updateGameEntity);

//        System.out.println(updateGameRequest);

        CloseableHttpResponse updateGameResponse = httpClient.execute(updateGameRequest);

        if (updateGameResponse.getStatusLine().getStatusCode() != 200) {
            System.out.println("Character guessed in game is not updated! Status code: " + updateGameResponse.getStatusLine().getStatusCode() + " - " + updateGameResponse.getStatusLine().getReasonPhrase());
        }

        HttpEntity responseEntity = updateGameResponse.getEntity();
        game = mapper.readValue(EntityUtils.toString(responseEntity), new TypeReference<Game>() {
        });

        //        System.out.println("Game: " + game.toString());
    }

    private static void updateTeamDb(int teamId) throws URISyntaxException, IOException {

        URIBuilder builder = new URIBuilder("http://localhost:8080/team/" + team.getTeamId());
        builder.addParameter("totalScore", Integer.toString(team.getTotalScore()));

        HttpPut updateGameRequest = new HttpPut(builder.build());
        StringEntity updateGameEntity = new StringEntity(mapper.writeValueAsString(updateGameRequest), ContentType.APPLICATION_JSON);
        updateGameRequest.setEntity(updateGameEntity);

//        System.out.println(updateGameRequest.toString());

        CloseableHttpResponse updateTeamResponse = httpClient.execute(updateGameRequest);

        if (updateTeamResponse.getStatusLine().getStatusCode() != 200) {
            System.out.println("Team's total score is not updated! Status code: " + updateTeamResponse.getStatusLine().getStatusCode() + " - " + updateTeamResponse.getStatusLine().getReasonPhrase());
        }

        HttpEntity responseEntity = updateTeamResponse.getEntity();
        team = mapper.readValue(EntityUtils.toString(responseEntity), new TypeReference<Team>() {
        });

        //        System.out.println("Tame: " + team.toString());
    }

    private static void getTeamByTeamId(int teamId) throws IOException {
        HttpGet getTeamByTeamIdRequest = new HttpGet("http://localhost:8080/team/" + teamId);

        CloseableHttpResponse getTeamByTeamIdResponse = httpClient.execute(getTeamByTeamIdRequest);
        HttpEntity responseEntity = getTeamByTeamIdResponse.getEntity();

        if (getTeamByTeamIdResponse.getStatusLine().getStatusCode() != 200) {
            System.out.println("Cannot get team by team ID of " + teamId + "! Status code: " + getTeamByTeamIdResponse.getStatusLine().getStatusCode() + " - " + getTeamByTeamIdResponse.getStatusLine().getReasonPhrase());
        }

        team = mapper.readValue(EntityUtils.toString(responseEntity), new TypeReference<Team>() {
        });

        //System.out.println("Team: " + team.toString());
    }

    public static void updateRoundDb() throws URISyntaxException, IOException {

//        round.setRoundId(31);
//        round.setSpinToken(true);
//        team.setTeamId(13);

//        System.out.println("TEAM ID: "+ team.getTeamId());
        URIBuilder builder = new URIBuilder("http://localhost:8080/round/" + round.getRoundId() + "/" + team.getTeamId());

//        System.out.println("updateRoundDb - roundScore: " + round.getRoundScore());
        builder.addParameter("roundScore", Integer.toString(round.getRoundScore()));

//        System.out.println("updateRoundDb - spinScore: " + round.getSpinScore());
        builder.addParameter("spinScore", Integer.toString(round.getSpinScore()));

//        System.out.println("updateRoundDb - spintToken: " + round.isSpinToken());
        builder.addParameter("spinToken", Boolean.toString(round.isSpinToken()));

        HttpPut updateRoundRequest = new HttpPut(builder.build());
        StringEntity updateGameEntity = new StringEntity(mapper.writeValueAsString(updateRoundRequest), ContentType.APPLICATION_JSON);
        updateRoundRequest.setEntity(updateGameEntity);

//        System.out.println(updateRoundRequest.toString());

        CloseableHttpResponse updateRoundResponse = httpClient.execute(updateRoundRequest);

        if (updateRoundResponse.getStatusLine().getStatusCode() != 200) {
            System.out.println("Round is not updated! Status code: " + updateRoundResponse.getStatusLine().getStatusCode() + " - " + updateRoundResponse.getStatusLine().getReasonPhrase());
        }

        HttpEntity responseEntity = updateRoundResponse.getEntity();
        RoundResponse roundResponse = mapper.readValue(EntityUtils.toString(responseEntity), new TypeReference<RoundResponse>() {
        });

//        System.out.println("RoundResponse: " + roundResponse.toString());
    }

    public static void getRoundByRoundIdTeamId(int roundId, int teamId) throws IOException {

//        round.setRoundId(18);
//        team.setTeamId(9);

        HttpGet getRoundByRoundIdTeamIdRequest = new HttpGet("http://localhost:8080/roundTeam/" + round.getRoundId() + "/" + team.getTeamId());

        CloseableHttpResponse getRoundByRoundIdTeamIdResponse = httpClient.execute(getRoundByRoundIdTeamIdRequest);
        HttpEntity responseEntity = getRoundByRoundIdTeamIdResponse.getEntity();

//        System.out.println("getRoundByRoundIdTeamId responseEntity: " + EntityUtils.toString(responseEntity));

        if (getRoundByRoundIdTeamIdResponse.getStatusLine().getStatusCode() != 200) {
            System.out.println("Cannot get round with round ID of " + round.getRoundId() + ", team ID of " + team.getTeamId() + " Status code: " + getRoundByRoundIdTeamIdResponse.getStatusLine().getStatusCode() + " - " + getRoundByRoundIdTeamIdResponse.getStatusLine().getReasonPhrase());
        }

        RoundResponse roundResponse = mapper.readValue(EntityUtils.toString(responseEntity), new TypeReference<RoundResponse>() {
        });

        round.setRoundId(RoundResponse.getRoundId().getRoundId());
        round.setTeamId(RoundResponse.getRoundId().getTeamId());
        round.setRoundScore(RoundResponse.getRoundScore());
        round.setSpinToken(RoundResponse.isSpinToken());

//        System.out.println("RoundResponse: " + roundResponse.toString());
    }

    public static void playTheGame(int teamCounter) throws URISyntaxException, IOException {

        while (controlWord.compareTo(wordToGuess) != 0) {

            boolean isNextTeam = false;
            boolean isGuessing = true;

            do {

                // before spinning wheel
                System.out.println();
                System.out.println(wordToGuess);
                System.out.println("Round Score: " + round.getRoundScore());

                boolean[] isGuessingIsNextTeamBooleans;

                isGuessingIsNextTeamBooleans = spinTheWheel(isGuessing, isNextTeam); // spin the wheel

                // handle 2 booleans
                isGuessing = isGuessingIsNextTeamBooleans[0];
                isNextTeam = isGuessingIsNextTeamBooleans[1];

//                System.out.println("spinTheWheel isGuessingIsNextTeamBooleans : " + isGuessing + ", " + isNextTeam);

                while (isGuessing) {
//                    System.out.println("guessingTheWord do while");
                    isGuessingIsNextTeamBooleans = guessingTheWord(isGuessing, isNextTeam); // guess the word

                    // handle 2 booleans
                    isGuessing = isGuessingIsNextTeamBooleans[0];
                    isNextTeam = isGuessingIsNextTeamBooleans[1];

//                    System.out.println("guessingTheWord isGuessingIsNextTeamBooleans : " + isGuessing + ", " + isNextTeam);
                }

                isGuessing = true;

            } while (!isNextTeam && controlWord.compareTo(wordToGuess) != 0); // keep guessing if not guess wrong and word is not complete

            if (isNextTeam && !wordToGuess.toString().equals(controlWord.toString())) { // change team if not guessing and word is not complete

                System.out.println();
                System.out.println("Your information: ");
                printContestantInformation();

                teamCounter = nextTeamFunction(teamCounter); // next team

                System.out.println();
                System.out.println("Next contestant information: ");
                printContestantInformation();

            }
        }

        if (controlWord.compareTo(wordToGuess) == 0) {
            System.out.println();
            System.out.println(wordToGuess);
            System.out.println("*****");
            System.out.println("You guessed correct!!!");
            System.out.println("*****");
        }
    }

    private static boolean[] guessingTheWord(boolean isGuessing, boolean isNextTeam) throws URISyntaxException, IOException {

        String guessedInput;
        boolean[] isGuessingIsNextTeamBooleans;

        // before guess
        System.out.println();
        System.out.println(wordToGuess);
        System.out.println("Round Score: " + round.getRoundScore());

        System.out.print("Guess a character: ");
        guessedInput = scan.nextLine().trim().toUpperCase();

        if (!guessedInput.isEmpty() && guessedInput.matches("^[\\sA-Z]+$")) { // if input is not empty and is alphabet

            isGuessingIsNextTeamBooleans = processingInput(isGuessing, isNextTeam, guessedInput);

        } else { // if input is empty or is not alphabet

            System.out.println();
            System.out.println("You must guess a letter.");

            isGuessingIsNextTeamBooleans = new boolean[]{true, false};
        }

        return isGuessingIsNextTeamBooleans;
    }

    private static boolean[] processingInput(boolean isGuessing, boolean isNextTeam, String guessedInput) throws URISyntaxException, IOException {

        boolean[] isGuessingIsNextTeamBooleans;

        if (guessedInput.length() > 1) { // if guess the word

            isGuessingIsNextTeamBooleans = processWord(isGuessing, isNextTeam, guessedInput);

        } else { // if guess character

            char charGuessed = guessedInput.charAt(0);
            isGuessingIsNextTeamBooleans = processChar(isGuessing, isNextTeam, charGuessed);
        }

        return isGuessingIsNextTeamBooleans;
    }

    private static boolean[] processChar(boolean isGuessing, boolean isNextTeam, char charGuessed) throws URISyntaxException, IOException {

        if (charGuessedSet.contains(charGuessed)) { // char is already guessed

            if (round.isSpinToken() == true) { // have token

                System.out.println();
                System.out.println(charGuessed + " has been guessed already....");
                System.out.println("How fortunate, you have a TOKEN. You can continue playing.");

                round.setSpinToken(false);
                round.setSpinScore(0);
                updateRoundDb();

                // spin and guess again
                isGuessing = false;
                isNextTeam = false;

            } else { // no token

                System.out.println();
                System.out.println(charGuessed + " has been guessed already... You lose your turn here.");
                System.out.println();

                round.setSpinScore(0);
                updateRoundDb();

                isGuessing = false; // no longer guessing
                isNextTeam = true; // next team
            }

        } else { // if new char

            Boolean isEnoughPoints = true;

            if (charInWordMap.containsKey(charGuessed)) { // guess correct

                if (vowels.contains(charGuessed)) { // buying a vowel

                    isEnoughPoints = calculateVowelPoint(isEnoughPoints);
                }

                if (isEnoughPoints) {

                    // calculate scoreWon (charNumber * spinScore)
                    List<Integer> charGuessedList = charInWordMap.get(charGuessed);
                    int charNumber = charGuessedList.size();
                    int scoreWon = charNumber * round.getSpinScore();

                    for (int i = 0; i < charGuessedList.size(); i++) {

//                        System.out.println("calculateCharGuessed - set wordToGuess after guess");
                        wordToGuess.setCharAt(charGuessedList.get(i), charGuessed);
//                        System.out.println("wordToGuess: " + wordToGuess);
                    }

                    charGuessedSet.add(charGuessed); // add the letter to charGuessedSet
                    updateCharGuessedInGameDb(charGuessed);

                    // update roundScore and reset spinScore
                    round.setRoundScore(round.getRoundScore() + scoreWon);
                    round.setSpinScore(0);
                    updateRoundDb();

//                    System.out.println();
//                    System.out.println(wordToGuess);
//                    System.out.println("Round score: " + round.getRoundScore());

                    // spin and guess again
                    isGuessing = false;

                } else { // not enough point

                    isGuessing = true; // guess again
                    isNextTeam = false;
                    ; // not changing team
                }

            } else { // guess wrong

                charGuessedSet.add(charGuessed); // add the letter to charGuessedSet
                updateCharGuessedInGameDb(charGuessed);

                if (round.isSpinToken() == true) { // have token

                    System.out.println();
                    System.out.println("You guessed wrong...");
                    System.out.println("Lucky, you have a TOKEN! You can continue playing.");

                    round.setSpinToken(false);
                    round.setSpinScore(0);
                    updateRoundDb();

                    // spin and guess again
                    isGuessing = false;
                    isNextTeam = false;

                } else { // don't have token

                    System.out.println();
                    System.out.println("Too bad! You guessed wrong! Next team turn.");
                    System.out.println();

                    round.setSpinScore(0);
                    updateRoundDb();

                    isGuessing = false; // no longer guessing
                    isNextTeam = true; // next team
                }
            }
        }

        return new boolean[]{isGuessing, isNextTeam};
    }

    private static boolean[] processWord(boolean isGuessing, boolean isNextTeam, String guessedInput) throws URISyntaxException, IOException {

        if (word.getWord().equals(guessedInput)) { // if guess the word correct, move on to next round

            // set roundScore based on spinScore
            round.setRoundScore(round.getRoundScore() + round.getSpinScore());
            round.setSpinScore(0);
            updateRoundDb();

//            // set totalScore based on roundScore
//            team.setTotalScore(team.getTotalScore() + round.getRoundScore());
//            updateTeamDb(team.getTeamId());

            // make wordToGuess's value equal to controlWord's value
            wordToGuess = new StringBuilder(controlWord);

            // no longer guessing current word
            isGuessing = false;
            isNextTeam = false;

        } else { // guessed the word wrong

            if (round.isSpinToken() == true) { // have token

                System.out.println();
                System.out.println("Congratulation... on guessing wrong!");
                System.out.println("You are so lucky, you have a token! You can continue playing.");

                round.setSpinToken(false);
                round.setSpinScore(0);
                updateRoundDb();

                // spin and guess again
                isGuessing = false;
                isNextTeam = false;

            } else { // no token

                // change team
                System.out.println();
                System.out.println("Too bad, you guessed wrong! Next team turn");
                System.out.println();
                System.out.println("Your information:");
                printContestantInformation();

                round.setSpinScore(0);
                updateRoundDb();

                isGuessing = false; // no longer guessing
                isNextTeam = true; // next team
            }
        }

        return new boolean[]{isGuessing, isNextTeam};
    }

    private static boolean calculateVowelPoint(boolean isEnoughPoints) {

        System.out.println();
        System.out.println("$$$ A vowel cost 250 points. $$$");
        System.out.println("You have " + round.getRoundScore() + " points.");

        if (round.getRoundScore() < 250) { // don't have enough point to buy vowel
            System.out.println();
            System.out.println("uhh...");
            System.out.println("Looks like you do not have enough point to buy a vowel.");
            System.out.println("Please guess another character.");

            isEnoughPoints = false;

        } else { // have enough point to buy vowel

            round.setRoundScore(round.getRoundScore() - 250); // buy a vowel
            System.out.println("You bought a vowel. You now have " + round.getRoundScore() + " point.");

            isEnoughPoints = true;
        }

        return isEnoughPoints;
    }

    private static void printContestantInformation() {
        System.out.println("=====");
        System.out.println("Name: " + team.getTeamName());
        System.out.println("Total Score: " + team.getTotalScore());
        System.out.println("Round Score: " + round.getRoundScore());
        System.out.println("Token: " + round.isSpinToken());
        System.out.println("=====");
    }

    public static int nextTeamFunction(int teamCounter) throws URISyntaxException, IOException {

        updateTeamDb(team.getTeamId()); // update current team in DB

        round.setSpinScore(0);
        updateRoundDb(); // update current round with current team in DB

//        System.out.println("teamList.size(): " + teamList.size());
//        System.out.println("teamCounter: " + teamCounter);

        // get next team using teamCounter
        if (teamCounter + 1 < teamList.size()) {
            teamCounter++; // next team in the list turn
//            System.out.println("incrementing teamCounter: " + teamCounter);

        } else {
            teamCounter = 0; // start at 0 index if next team turn is bigger than the teamList size
//            System.out.println("resetting: " + teamCounter);
        }

        // change team instance to next team by getting next team info from db
//        System.out.println("teamCounter: " + teamCounter);
        getTeamByTeamId(teamList.get(teamCounter).getTeamId());

        // change round instance based on roundId teamID
        getRoundByRoundIdTeamId(round.getRoundId(), team.getTeamId());

        // update game DB
        updateGameDb();

        return teamCounter;
    }

    public static void processChosenWord() {

//        String wordChosen = "BANANA";
        String wordChosen = word.getWord();

//        System.out.println("wordChosen: " + wordChosen);
//        game.getWord().setWord(w.toUpperCase());
//        String wordChosen = game.getWord().getWord();

//        System.out.println("-0 controlWord: " + controlWord);
        controlWord = setControlWord(wordChosen);
//        System.out.println("0 controlWord: " + controlWord);
        wordToGuess = setWordToGuessAndCharInWordMap(controlWord);

//        System.out.println("controlWord: " + controlWord);
//        System.out.println("wordToGuess: " + wordToGuess);
//
//        System.out.println(charInWordMap.toString());
//        System.out.println(controlWord);
//        System.out.println(controlWord.length() - 1);
//        System.out.println(wordToGuess);
//        System.out.println(wordToGuess.length() - 1);
    }

    private static StringBuilder setWordToGuessAndCharInWordMap(StringBuilder controlWord) {

        wordToGuess = new StringBuilder(controlWord);

        for (int i = 0; i < wordToGuess.length(); i++) {

            if (wordToGuess.charAt(i) >= 'A' && wordToGuess.charAt(i) <= 'Z') {

                // set charInWordMap
                if (!charInWordMap.containsKey(wordToGuess.charAt(i))) {
                    charInWordMap.put(wordToGuess.charAt(i), new ArrayList<>()); // instantiate arrayList at the first char
                    charInWordMap.get(wordToGuess.charAt(i)).add(i); // add the first index to the value of map
                } else {
                    charInWordMap.get(wordToGuess.charAt(i)).add(i);
                }

                // set wordToGuess
                wordToGuess.setCharAt(i, '_');
            }
        }

        return wordToGuess;
    }

    private static StringBuilder setControlWord(String wordChosen) {

        for (int i = 0; i < wordChosen.length(); i++) {

            char currentChar = wordChosen.charAt(i);

            if (symbolsWithSpaceAfter.contains(currentChar)) { // if symbols allows space after
                controlWord.append(currentChar);

            } else if (symbolsNoSpaceAfter.contains(currentChar)) { // if symbols don't allow space after then check if char after is alphabet
                controlWord.append(currentChar);

                // if char after ' or - is an alphabet and if i < wordChosen length, add space after
                if ((i + 1) < wordChosen.length() && wordChosen.charAt(i + 1) >= 'A' && wordChosen.charAt(i + 1) <= 'Z') {
                    controlWord.append(wordChosen.charAt(i + 1));
                    i++;
                }
            } else if (i == 0) {
                controlWord.append(wordChosen.charAt(0));
            } else if (wordChosen.charAt(i) == ' ') {
                controlWord.append("  ");
            } else {
                controlWord.append(" " + currentChar);
            }

//            System.out.println("1 controlWord: " + controlWord);
//            System.out.println("1 currentChar: " + currentChar);
        }

        return controlWord;
    }

    public static boolean[] spinTheWheel(boolean isGuessing, boolean isNextTeam) throws IOException, URISyntaxException {

        Random rand = new Random();
        String spinResultString = wheel.get(rand.nextInt(wheel.size()));

        // spin the wheel
        System.out.println();
        System.out.println("=====");
        System.out.println("Spin The Wheel!");
        System.out.println("Choices are 500, 550, 600, 650, 700, 900, 1000, 3500, Bankrupt, Lose A Turn, and Spin Token.");
        System.out.println("Press 'ENTER' to spin the wheel.");
        scan.nextLine();

        if (noneNumWheelResult.contains(spinResultString)) {

            System.out.println("=====");
            System.out.println("You landed on " + spinResultString + ".");

            if (spinResultString == "TOKEN") {

                System.out.println();
                System.out.println("Congratulation on getting a TOKEN!");
                System.out.println("This token will be used when you guess wrong, you guess a character that has already been guessed, or went bankrupt.");

                round.setSpinToken(true);
                updateRoundDb();

                // move on to guessing
                isNextTeam = false;
                isGuessing = true;

            } else if (spinResultString == "LOSE A TURN") {

                if (round.isSpinToken() == true) { // have token

                    System.out.println();
                    System.out.println("Your turn should end here...");
                    System.out.println("However, you have a TOKEN! You can continue playing.");

                    round.setSpinToken(false);
                    updateRoundDb();

                    // move on to guessing
                    isGuessing = true;
                    isNextTeam = false;

                } else { // no token

                    System.out.println();
                    System.out.println("How unfortunate... Your turn ended here. Better luck next time.");

                    round.setSpinScore(0);
                    updateRoundDb();

                    // next team
                    isGuessing = false; // can't guess
                    isNextTeam = true;
                }

            } else if (spinResultString == "BANKRUPT") {

                if (round.isSpinToken() == true) { // have token

                    System.out.println();
                    System.out.println("Seem like you went BANKRUPT...  You loose all your money from this round and your turn end here.");
                    System.out.println("However, you have a TOKEN! You can continue playing.");

                    round.setSpinToken(false);
                    updateRoundDb();

                    // continue to guessing
                    isGuessing = true;
                    isNextTeam = false;

                } else { // no token

                    System.out.println();
                    System.out.println("You went BANKRUPT! You lose all your money from this round. You are too broke, your turn end here");

                    round.setSpinScore(0);
                    round.setRoundScore(0);
                    updateRoundDb();

                    // next team
                    isGuessing = false;
                    isNextTeam = true;
                }
            }

        } else { // if spinResult is number

            int spinResultInt = Integer.parseInt(spinResultString);

            System.out.println();
            System.out.println("You landed on " + spinResultInt + " points!");

            round.setSpinScore(spinResultInt);
            updateRoundDb();

            // continue to guessing
            isGuessing = true;
            isNextTeam = false;
        }

        return new boolean[]{isGuessing, isNextTeam};
    }
}

