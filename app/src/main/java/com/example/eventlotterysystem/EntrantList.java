package com.example.eventlotterysystem;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This is a class that Contains Entrant Information related to associated Event
 * @deprecated
 */
public class EntrantList {
    private int signUpLimit;
    private int confirmedLimit;
    private final List<EntrantInfo> waitingList = new ArrayList<>();
    private final List<EntrantInfo> chosenList = new ArrayList<>();
    private final List<EntrantInfo> confirmedList = new ArrayList<>();

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

    //TODO
    public void Chosing() {
        //moves random users from waitingList to choosenList
    }
    /**
     * This signs up a Entrant into the waitingList
     * @param Entrant
     * This is the Entrant to be signed up
     * @return true if add was successful
     * @throws IllegalArgumentException if Entrant is already registered
     */
    public boolean addEntrantToWaitingList(EntrantInfo Entrant) {
        for (EntrantInfo EntrantInWaitingList: waitingList) {
            if (EntrantInWaitingList.getId().equals(Entrant.getId())) {
                throw new IllegalArgumentException("Entrant already in Waiting List");
            }
        }
        for (EntrantInfo EntrantInChosenList: chosenList) {
            if (EntrantInChosenList.getId().equals(Entrant.getId())) {
                throw new IllegalArgumentException("Entrant already is selected");
            }
        }
        for (EntrantInfo EntrantInConfirmedList: confirmedList) {
            if (EntrantInConfirmedList.getId().equals(Entrant.getId())) {
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
    public boolean confirmEntrant(EntrantInfo Entrant) {
        for (Iterator<EntrantInfo> iterator = chosenList.iterator(); iterator.hasNext();) {
            EntrantInfo EntrantInChosenList = iterator.next();
            if (EntrantInChosenList.getId().equals(Entrant.getId())) {
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

    public int getTotalEntrants() {
        return waitingList.size() + chosenList.size() + confirmedList.size();
    }

    public List<EntrantInfo> getWaitingList() {
        return waitingList;
    }

    public List<EntrantInfo> getChoosenList() {
        return chosenList;
    }

    public List<EntrantInfo> getConfirmedList() {
        return confirmedList;
    }

    //Temporary Method
    public void addChosen(EntrantInfo Entrant) {
        chosenList.add(Entrant);
    }
}
