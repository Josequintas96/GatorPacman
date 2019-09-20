package edu.ufl.cise.cs1.controllers;

import game.controllers.AttackerController;
import game.models.*;

import java.util.List;

public final class StudentAttackerController implements AttackerController {
    int n = 0;
    int numbpowerlist;
    String levelname;

    public void init(Game game) {
        levelname = game.getCurMaze().getName();
        numbpowerlist = game.getCurMaze().getNumberPowerPills() - 1;
    }

    public void shutdown(Game game) {
    }

    //run away from a defender if it is a certain distance between him and the closest defender
    public int runfromit(Game game, int action, Defender defn, double nearest) {
        int newaction = action;
        if (nearest != 1) {
            if ( nearest <= 4 && defn.getDirection() != action) //only risk of collision,
                            // if the defender is follow gator or gator is following defender (same direction), then nothing happen
            {
                newaction = game.getAttacker().getNextDir(defn.getLocation(), false);
            }
        }

        return newaction;
    }

    //move toward the closest pill in the field
    public int eatpill(Game game)
    {
        int newaction = game.getAttacker().getNextDir
                (game.getAttacker().getTargetNode(game.getPillList(), true), true);
        return newaction;
    }

    //move toward the closest power pill in the field
    public int eatpowerpill(Game game)
    {
        int newaction = game.getAttacker().getNextDir
                (game.getAttacker().getTargetNode(game.getPowerPillList(), true), true);
        return newaction;
    }

    //this method will attacker the closest defender only if it is safe
    public int goforit(Game game, int action, Defender defn) // attack closest one
    {
        int run = action;
        if (defn.isVulnerable())// secret fail-safe
        {
            run = game.getAttacker().
                    getNextDir(defn.getLocation(), true);
        }
        return run;
    }

    //calculate the distance between the attacker and the defender
    //using Manhatan Theorem
    public double ManhatanDistance(Game game, Defender defn) {
        double manhatandistance = 0;
        int x1 = game.getAttacker().getLocation().getX();
        int y1 = game.getAttacker().getLocation().getY();
        int x2 = defn.getLocation().getX();
        int y2 = defn.getLocation().getY();
        manhatandistance = Math.abs(x1 - x2) + Math.abs(y1 - y2);
        return manhatandistance;
    }


    //calculate the distance between the attacker and the defender, it will exclude any -1 distance.
    // it will return the "real" closest defender; if defender is in lir, it is consider the closest defender
    //even if it is not.
    public Defender minDistance(Game game) {
        Defender defn[] = game.getDefenders().toArray(new Defender[0]);
        int distance[] = new int[4];
        distance[0] = game.getAttacker().getLocation().getPathDistance
                (defn[0].getLocation());
        distance[1] = game.getAttacker().getLocation().getPathDistance
                (defn[1].getLocation());
        distance[2] = game.getAttacker().getLocation().getPathDistance
                (defn[2].getLocation());
        distance[3] = game.getAttacker().getLocation().getPathDistance
                (defn[3].getLocation());

        int minD = 999;
        int k = 0;

        for (int i = 0; i < defn.length; i++) {
            if (distance[i] >= 0 && distance[i] < minD) {
                for (int t = i + 1; t < defn.length; t++) {
                    if (distance[i] < distance[t] && distance[t] >= 0) {
                        minD = distance[i];
                        k = i;
                    }
                }
            }
        }

        return defn[k];
    }

    //gator stay in same node until there is a defender close to him, then the gator will eat the closest power pill
    //start counter attack
    public int stopthere(Game game, int action, Defender defn, double distance)
    {
        int newaction = action;
        if (game.getAttacker().getLocation().getPathDistance
                (game.getAttacker().getTargetNode(game.getPowerPillList(), true)) < 4) {
            newaction = game.getAttacker().getReverse();

            if (distance <= 7)
            {
                newaction = eatpowerpill(game);
            }

        }
        return newaction;
    }

    public int update(Game game, long timeDue) {
        int action = -1;
        //PHASE 1
        //Step 1: current level
        String currentlevel = game.getCurMaze().getName();
        if (levelname.equals(currentlevel) == false) {
            levelname = game.getCurMaze().getName();
            numbpowerlist = game.getCurMaze().getNumberPowerPills() - 1;
            if(levelname.equals("B"))
            {
                n = 1;
            }
            else
            {
                n=0;
            }
        }

        //step 2: Find closest and farthest defender
        Defender defn = (Defender) game.getAttacker().getTargetActor(game.getDefenders(), true);
        Defender defn2 = (Defender) game.getAttacker().getTargetActor(game.getDefenders(), false);

        //step 3. distance with closest defender
        double closest = game.getAttacker().getLocation().getPathDistance(defn.getLocation());

        //step 4. make sure of defender distance
        if (closest == -1) {
            defn = minDistance(game);
            closest = game.getAttacker().getLocation().getPathDistance(defn.getLocation());
            if (closest == -1)  //ultimate fail-safe in case minDistance still provide me
                                // with wrong closest
            {
                closest = ManhatanDistance(game, defn);
            }
        }

        //step 5. have you pass through power pill
        if (game.getAttacker().getLocation().isPowerPill() == false) {
            numbpowerlist = numbpowerlist - 1;
        }

        //phase 2
        //step 6. eat pill
        if(n!=1) {

            action = eatpill(game);

            //7. runaway mister coward
            action = runfromit(game, action, defn, closest);

            //8 is vulnerable??
            if (defn.isVulnerable() || defn2.isVulnerable())
            {
                //9. is vulnerable, it is killing time
                if (defn.getVulnerableTime() == 1 || defn2.getVulnerableTime() >= 1)
                {
                    if (defn.isVulnerable() && closest <= 10)
                    {
                        action = goforit(game, action, defn);
                    }
                }
            } else
                {
                if (numbpowerlist == 1)
                //step 10. wait there, here is the last change to gain point,
                    // you are the bait, when defenders are close, you will eat power pills and them eat all of them
                {
                    action = stopthere(game, action, defn, closest);
                }
            }
        }
        else
        {
            //if you are in level 2, then it is time to change the game
            //step 6. if there is one or 3 power pills on the maze, then go for them
            //I choose 1 and 3 to avoid eating all of the power pills at once
            if(numbpowerlist == 3 || numbpowerlist == 1)
            {
                action = eatpowerpill(game);

            }
            else
            {
                //step 7. if you aren't going for a power pill, then go for the closest pill
                action = eatpill(game);
            }

            //step 8. run away mister coward.
            action = runfromit(game, action, defn, closest);

            //Step 9. if the closest or farthest defender are vulnerable, then it recognize that vulnerable mode is active
            if (defn.isVulnerable() || defn2.isVulnerable()) {
                if (defn.getVulnerableTime() == 1 || defn2.getVulnerableTime() >= 1)
                {
                    //step 10. attack if defender if it is vulnerable and is close
                    if (defn.isVulnerable() && closest <= 16) //here there is a higher distance for acceptable vulnerable defender
                                                                // because i am trying to increase the score
                    {
                        action = goforit(game, action, defn);
                    }
                }
            }
            else // if vulnerable mode isn't active, then if the gator get in front of a power pill, the gator will stop
                {
                    //step 10. stop at power pill step
                if (numbpowerlist >= 1) {
                    action = stopthere( game, action, defn, closest);
                }
            }
        }

            return action;

        }
}
