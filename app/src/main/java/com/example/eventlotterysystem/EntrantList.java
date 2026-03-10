package com.example.eventlotterysystem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * This is a class that Contains Entrant Information related to associated Event
 */
public class EntrantList {
    private int signUpLimit;
    private int confirmedLimit;
    private List<UserProfile> waitingList = new ArrayList<>();
    private List<UserProfile> choosenList = new ArrayList<>();
    private List<UserProfile> confirmedList = new ArrayList<>();

    /**
     * This creates the EntrantList object
     * @param signLimit
     * This is number of Entrants able to sign up for the Event
     * @param confirmedLimit
     * This is number of slots available to Entrants
     */
    public EntrantList(int signLimit, int confirmedLimit) {
        this.signUpLimit = signLimit;
        this.confirmedLimit = confirmedLimit;
    }
    /**
     * This creates the EntrantList object
     * @param confirmedLimit
     * This is number of slots available to Entrants
     */
    public EntrantList(int confirmedLimit) {
        this.signUpLimit = -1;
        this.confirmedLimit = confirmedLimit;
    }

    /**
     * This is the lottery draw function. it shuffles the waitingList,
     * then takes the first entrant from the list and moves them to choosenList
     * this continues until the choosenList.size == confirmedLimit
     * the entrants are removed from the waitingList
     * This is number of slots available to Entrants
     */
    public void Choosing() {
        //randomly choose entrants from waitinglist to put in choosenllist
        Collections.shuffle(this.waitingList);
        int countToDraw = Math.min(this.confirmedLimit, this.waitingList.size());
        if(this.choosenList.size() < confirmedLimit){
            for(int i = 0; i < countToDraw; i++){
                UserProfile chosenOne = this.waitingList.get(0);
                addChoosen(chosenOne);
                //remove chosenOne from waitingList
                this.waitingList.remove(chosenOne);

            }
        }


    }
    /**
     * This signs up a Entrant into the waitingList
     * @param Entrant
     * This is the Entrant to be signed up
     * @return true if add was successful
     * @throws IllegalArgumentException if Entrant is already registered
     */
    public boolean addEntrantToWaitingList(UserProfile Entrant) {
        for (UserProfile EntrantInWaitingList: waitingList) {
            if (EntrantInWaitingList.getUsernameKey() == Entrant.getUsernameKey()) {
                throw new IllegalArgumentException("Entrant already in Waiting List");
            }
        }
        for (UserProfile EntrantInChoosenList: choosenList) {
            if (EntrantInChoosenList.getUsernameKey() == Entrant.getUsernameKey()) {
                throw new IllegalArgumentException("Entrant already is selected");
            }
        }
        for (UserProfile EntrantInConfirmedList: confirmedList) {
            if (EntrantInConfirmedList.getUsernameKey() == Entrant.getUsernameKey()) {
                throw new IllegalArgumentException("Entrant already confirmed for event");
            }
        }
        if (signUpLimit >= 0 && waitingList.size() >= signUpLimit) {
            return false;
        }
        else {
            waitingList.add(Entrant);
            return true;
        }

    }

    /**
     * This confirms a selected Entrant is attending the Event
     * @param Entrant
     * This is the Entrant that confirmed
     * @return true if confirmation was successful
     */
    public boolean confirmEntrant(UserProfile Entrant) {
        for (Iterator<UserProfile> iterator = choosenList.iterator(); iterator.hasNext();) {
            UserProfile EntrantInChoosenList = iterator.next();
            if (EntrantInChoosenList.getUsernameKey().equals(Entrant.getUsernameKey())) {
                iterator.remove();
                confirmedList.add(Entrant);
                return true;
            }
        }
        return false;
    }

    //TODO
    public void ExportConfimed() {
        // create a CSV file
    }

    /**
     * This removes the signup limit for entrants if there was one
     */
    public void removeSignUpLimit() {
        this.signUpLimit = -1;
    }

    public int getSignUpLimit() {
        return signUpLimit;
    }

    /**
     * This sets/changes the signup limit for entrants
     * does not remove Entrants from waitingList even if over limit
     */
    public void setSignUpLimit(int signUpLimit) {
        this.signUpLimit = signUpLimit;
    }

    public int getConfirmedLimit() {
        return confirmedLimit;
    }

    public void setConfirmedLimit(int confirmedLimit) {
        this.confirmedLimit = confirmedLimit;
    }

    public List<UserProfile> getWaitingList() {
        return waitingList;
    }

    public List<UserProfile> getChoosenList() {
        return choosenList;
    }

    public List<UserProfile> getConfirmedList() {
        return confirmedList;
    }

    //Temporary Method
    public void addChoosen(UserProfile Entrant) {
        choosenList.add(Entrant);
    }
}
