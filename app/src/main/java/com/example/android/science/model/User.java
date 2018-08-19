package com.example.android.science.model;

import java.util.Map;

/**
 * Created by Cristina on 14/08/2018.
 * This POJO class represents a single user. Each object has information about the
 * user, such as name, email, profile photo or stats.
 */
public class User {
    public String username, email, nickname, photoUrl;
    public int numberOfGames, correctAnswers, incorrectAnswers, totalPoints;
    public Map<String, Integer> pointsByCategory;

    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public User(String username, String email, String nickname, String photoUrl, int numberOfGames,
                int correctAnswers, int incorrectAnswers, int totalPoints,
                Map<String, Integer> pointsByCategory) {
        this.username = username;
        this.email = email;
        this.nickname = nickname;
        this.photoUrl = photoUrl;
        this.numberOfGames = numberOfGames;
        this.correctAnswers = correctAnswers;
        this.incorrectAnswers = incorrectAnswers;
        this.totalPoints = totalPoints;
        this.pointsByCategory = pointsByCategory;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public int getNumberOfGames() {
        return numberOfGames;
    }

    public void setNumberOfGames(int numberOfGames) {
        this.numberOfGames = numberOfGames;
    }

    public int getCorrectAnswers() {
        return correctAnswers;
    }

    public void setCorrectAnswers(int correctAnswers) {
        this.correctAnswers = correctAnswers;
    }

    public int getIncorrectAnswers() {
        return incorrectAnswers;
    }

    public void setIncorrectAnswers(int incorrectAnswers) {
        this.incorrectAnswers = incorrectAnswers;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(int totalPoints) {
        this.totalPoints = totalPoints;
    }

    public Map<String, Integer> getPointsByCategory() {
        return pointsByCategory;
    }

    public void setPointsByCategory(Map<String, Integer> pointsByCategory) {
        this.pointsByCategory = pointsByCategory;
    }
}
