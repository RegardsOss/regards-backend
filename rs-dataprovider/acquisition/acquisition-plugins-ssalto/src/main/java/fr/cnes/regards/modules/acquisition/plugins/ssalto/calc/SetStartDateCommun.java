/*
 * $Id$
 *
 * HISTORIQUE
 *
 * VERSION : 2012/03/23 : 2.0 : CS 
 * DM-ID : SIPNG-DM-0105-CN : 2012/03/23 : Creation 
 *
 * FIN-HISTORIQUE
 */
package fr.cnes.regards.modules.acquisition.plugins.ssalto.calc;


/**
 * This classe calculates a date according to the input value.<br>
 * The required value format is "yyyy  mm dd hh mm ss" or "yyyy mm dd" 

 * @author CS
 * @version 4.7.3
 * @since 4.7.3
 */

public class SetStartDateCommun extends AbstractSetDateCommun {

	int getDefaultHour() {
		return 0;
	}

	int getDefaultMinute() {
		return 0;
	}

	int getDefaultSecond() {
		return 0;
	}	
}