package com.sophieopenclass.go4lunch;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.Query;
import com.sophieopenclass.go4lunch.models.Message;
import com.sophieopenclass.go4lunch.models.Restaurant;
import com.sophieopenclass.go4lunch.models.User;
import com.sophieopenclass.go4lunch.models.json_to_java.PlaceDetails;
import com.sophieopenclass.go4lunch.models.json_to_java.RestaurantsResult;
import com.sophieopenclass.go4lunch.repository.AlgoliaDataRepository;
import com.sophieopenclass.go4lunch.repository.ChatDataRepository;
import com.sophieopenclass.go4lunch.repository.RestaurantDataRepository;
import com.sophieopenclass.go4lunch.repository.UserDataRepository;

import java.util.ArrayList;
import java.util.List;

public class MyViewModel extends ViewModel {
    private RestaurantDataRepository restaurantDataSource;
    private UserDataRepository userDataSource;
    private ChatDataRepository chatDataSource;
    private LiveData<User> createdUserLiveData;
    private AlgoliaDataRepository algoliaDataSource;

    public MyViewModel(RestaurantDataRepository restaurantDataSource, UserDataRepository userDataSource,
                       ChatDataRepository chatDataSource, AlgoliaDataRepository algoliaDataSource) {
        this.restaurantDataSource = restaurantDataSource;
        this.userDataSource = userDataSource;
        this.chatDataSource = chatDataSource;
        this.algoliaDataSource = algoliaDataSource;
    }

    // RESTAURANTS
                // -- PLACES API
    public LiveData<RestaurantsResult> getNearbyPlaces(String location) {
        return restaurantDataSource.getNearbyPlaces(location);
    }

    public LiveData<RestaurantsResult> getMoreNearbyPlaces(String pageToken) {
        return restaurantDataSource.getMoreNearbyPlaces(pageToken);
    }

    public LiveData<PlaceDetails> getPlaceDetails(String placeId, String language) {
        return restaurantDataSource.getPlaceDetails(placeId, language);
    }

    public LiveData<List<PlaceDetails>> getPlaceDetailsList(List<String> placeIds, String language) {
        return restaurantDataSource.getPlaceDetailsList(placeIds, language);
    }

                // -- FIREBASE
    public LiveData<Restaurant> updateChosenRestaurant(String uid, Restaurant restaurant, String date) {
        return userDataSource.updateChosenRestaurant(uid, restaurant, date);
    }

    public void deleteChosenRestaurant(String uid, String date) {
        userDataSource.deleteChosenRestaurant(uid, date);
    }

    public void addRestaurantToFavorites(Restaurant restaurant, String userId) {
        userDataSource.addRestaurantToFavorites(restaurant, userId);
    }

    public void deleteRestaurantFromFavorites(String userId, String placeId) {
        userDataSource.deleteRestaurantFromFavorites(userId, placeId);
    }

    // USERS
    public void createUser(User user) {
        createdUserLiveData = userDataSource.createUser(user);
    }

    public LiveData<User> getCreatedUserLiveData() {
        return createdUserLiveData;
    }

    public LiveData<User> getUser(String uid) {
        return userDataSource.getUser(uid);
    }

    public Query getUsersEatingAtRestaurantQuery(String placeId) {
        return userDataSource.getUsersEatingAtRestaurantQuery(placeId);
    }

    public LiveData<List<User>> getListUsers() {
        return userDataSource.getListUsers();
    }

    public LiveData<List<User>> getUsersEatingAtRestaurantToday(String placeId, String date) {
        return userDataSource.getUsersEatingAtRestaurantToday(placeId, date);
    }

    public LiveData<String> updateUsername(String username, String uid) {
        return userDataSource.updateUsername(username, uid);
    }

    public void deleteUser(String uid) {
        userDataSource.deleteUser(uid);
    }

    public void populateAlgolia(List<User> workmates) {
        algoliaDataSource.populateDatabase(workmates);
    }

    public LiveData<ArrayList<User>> searchWorkmate(String input) {
        return algoliaDataSource.searchWorkmate(input);
    }

    // CHAT
    public LiveData<Message> createMessageForChat(String textMessage, String userSenderId, String chatId) {
        return chatDataSource.createMessageForChat(textMessage, userSenderId, chatId);
    }

    public MutableLiveData<Message> createMessageWithImageForChat(String urlImage, String textMessage
            , String userSenderId, String chatId) {
        return chatDataSource.createMessageWithImageForChat(urlImage, textMessage, userSenderId, chatId);
    }

    public LiveData<String> getChatId(String currentUserId, String workmateId) {
        return chatDataSource.getChatId(currentUserId, workmateId);
    }


    public Query getMessagesQuery(String chatId) {
        return chatDataSource.getMessagesQuery(chatId);
    }

    public LiveData<Boolean> createChat(String currentUserId, String workmateId) {
        return chatDataSource.createChat(currentUserId, workmateId);
    }

    public void deleteUserMessages(String uid) {
        chatDataSource.deleteUserMessages(uid);
    }

    public LiveData<String> updateUserUrlPicture(String urlPicture, String uid) {
        return userDataSource.updateUserUrlPicture(urlPicture, uid);
    }
}
