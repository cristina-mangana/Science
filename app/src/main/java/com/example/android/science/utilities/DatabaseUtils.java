package com.example.android.science.utilities;

/**
 * Created by Cristina on 14/08/2018.
 * Variables related with the database nodes and endpoints.
 */
public class DatabaseUtils {
    public static final String USERS_REFERENCE = "users";
    public static final String USERNAME = "/username";
    public static final String PHOTO_URL = "/photoUrl";
    public static final String NUMBER_OF_GAMES = "/numberOfGames";
    public static final String CORRECT_ANSWERS = "/correctAnswers";
    public static final String INCORRECT_ANSWERS = "/incorrectAnswers";
    public static final String TOTAL_POINTS = "/totalPoints";
    public static final String POINTS_BY_CATEGORY_REFERENCE = "/pointsByCategory";
    public static final String GENERAL_CATEGORY = "category_general";
    public static final String MATHS_CATEGORY = "category_maths";
    public static final String COMPUTERS_CATEGORY = "category_computers";
    public static final String ANIMALS_CATEGORY = "category_animals";

    public static final String[] categoryNames = {GENERAL_CATEGORY, MATHS_CATEGORY,
            COMPUTERS_CATEGORY, ANIMALS_CATEGORY};
}
