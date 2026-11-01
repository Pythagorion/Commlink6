package de.rpgframework.shadowrun6;

import de.rpgframework.genericrpg.chargen.Rule;
import de.rpgframework.genericrpg.chargen.Rule.EffectOn;
import de.rpgframework.genericrpg.data.RuleFlag;

/**
 * Flags how to handle specific rules in a character. They originate from
 * qualities, adept powers and other elements that change how rules apply
 * to a character.
 * This flags are not preconfigured, but injected by modifications.
 *
 * @author prelle
 *
 */
public enum SR6RuleFlag implements RuleFlag {

	// Convert 1 Karma to 5000 Nuyen
	IN_DEBT,
	UNARMED_DAMAGE_IS_PHYSICAL,
	// Physical damage gets converted to stun damage
	PHYSICAL_DAMAGE_IS_STUN,
	/* Lower effects of damage */
	PAIN_TOLERANCE_LOWER_MOD,
	/* Double effects of damage */
	PAIN_TOLERANCE_DOUBLE_MOD,
	/* Ignore damage box before calculating modifiers */
	PAIN_TOLERANCE_IGNORE_BOX,

	DECREASE_AVAILABILITY_2,

	// Apply Adept and Magican effects of the mentor spirit
	MENTOR_SPIRIT_BOTH,
	/* Use CHA for defense rating */
	CHARISMATIC_DEFENSE,
	ADD_2_TWO_LOWEST_CYBERJACK_ATTRIBUTES,
	/** Discount of 2 Karma for martial arts or technique */
	MARTIAL_ARTS_PRODIGY,
	/**
	 * You may learn new language skills at a cost of 2 Karma per rank.
	 * You may improve language skills for only 1 Karma per rank.
	 * You may improve language skills up to rank 4 (native).
	 */
	POLYGLOT,
	MAX_3_CONTACT_LOYALTY,
	DECK_5000_PER_KARMA,
	KNOWLEDGE_COST2_AT_GEN,
	// Power Plays (Quality Fashion Influencer)
	FASHION_50_PERCENT,

	// 6WC: Physical monitor round down (e.g. Neoteny from 6WC)
	PHYSICAL_ROUND_DOWN,

	/** Used and standard cyberware gets upgraded by one */
	CYBERADEPT_NOVICE,
	/** Standard and alphacyberware gets upgraded by one (in addition to NOVICE) */
	CYBERADEPT_DISCIPLE,
	CYBERADEPT_MASTER,
	CANNOT_USE_ESSENCE_HOLE,
	ESSENCE_REGROWTH,

	/** BS 168: Pair of cyberlimbs increase willpower */
	CYBER_SINGULARITY_SEEKER,
	/** BS 168: Pair of cyberlimbs increases STR+AGI (up to +2), but only half PHYSICAL_MONITOR */
	REDLINER,
	
	// SO 162
	CONTACTS_START_LOYAL2,


}
