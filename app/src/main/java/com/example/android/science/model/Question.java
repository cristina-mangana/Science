package com.example.android.science.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

/**
 * Created by Cristina on 09/08/2018.
 * This POJO class represents a single trivia question. Each object has information about the
 * question, such as title, correct answer and incorrect answers.
 */
public class Question implements Parcelable {
    private String mQuestionTitle, mCorrectAnswer;
    private List<String> mIncorrectAnswers;

    /**
     * No arguments constructor to construct the object with the setter methods
     */
    public Question() {
    }

    // De-parcel object
    protected Question(Parcel in) {
        mQuestionTitle = in.readString();
        mCorrectAnswer = in.readString();
        mIncorrectAnswers = in.createStringArrayList();
    }

    // Creator
    public static final Creator<Question> CREATOR = new Creator<Question>() {
        @Override
        public Question createFromParcel(Parcel in) {
            return new Question(in);
        }

        @Override
        public Question[] newArray(int size) {
            return new Question[size];
        }
    };

    /**
     * Get the question title
     */
    public String getQuestionTitle() {
        return mQuestionTitle;
    }

    /**
     * Get the correct answer
     */
    public String getCorrectAnswer() {
        return mCorrectAnswer;
    }

    /**
     * Get the list of incorrect answers
     */
    public List<String> getIncorrectAnswers() {
        return mIncorrectAnswers;
    }

    /**
     * Set the question title
     */
    public void setQuestionTitle(String questionTitle) {
        this.mQuestionTitle = questionTitle;
    }

    /**
     * Set the correct answer
     */
    public void setCorrectAnswer(String correctAnswer) {
        this.mCorrectAnswer = correctAnswer;
    }

    /**
     * Set the list of incorrect answers
     */
    public void setIncorrectAnswers(List<String> incorrectAnswers) {
        this.mIncorrectAnswers = incorrectAnswers;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(mQuestionTitle);
        parcel.writeString(mCorrectAnswer);
        parcel.writeStringList(mIncorrectAnswers);
    }
}
