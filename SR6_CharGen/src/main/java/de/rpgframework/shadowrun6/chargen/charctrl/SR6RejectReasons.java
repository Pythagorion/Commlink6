package de.rpgframework.shadowrun6.chargen.charctrl;

import java.util.Locale;

import de.rpgframework.MultiLanguageResourceBundle;
import de.rpgframework.shadowrun.chargen.charctrl.IRejectReasons;

/**
 * @author prelle
 *
 */
public interface SR6RejectReasons extends IRejectReasons {

	public final static MultiLanguageResourceBundle RES = new MultiLanguageResourceBundle(SR6RejectReasons.class, Locale.ENGLISH, Locale.GERMAN);

	public final static String IMPOSS_QUALITY_ALREADY_6 = "impossible.qualityAlready6";
	public final static String IMPOSS_QUALITY_KARMAGAIN = "impossible.qualityMoreThan20Karma";
	public final static String IMPOSS_ALREADY_MAX_LIMIT = "impossible.cannotMaxMoreAttributes";
	public final static String IMPOSS_LIFEPATH_MODULE_LIMIT = "impossible.lifepath.moduleLimit";
	public final static String IMPOSS_LIFEPATH_MODULE_REQUIRED = "impossible.lifepath.moduleRequired";
	public final static String IMPOSS_LIFEPATH_SELMOD_MISSING = "impossible.lifepath.selmodMissing";
	public final static String IMPOSS_REQUIREMENTS_NOT_MET = "impossible.requirementsNotMet";

	public final static String TODO_ATTRIB_REMAIN_ADJUST = "impossible.remainingAdjustmentPoints";
	public final static String TODO_ATTRIB_REMAIN_ATTRIB = "impossible.remainingAttributePoints";

	public final static String TODO_SKILL_REMAIN_POINTS  = "impossible.remainingSkillPoints";
	public final static String TODO_SKILL_REMAIN_POINTS2 = "impossible.remainingKnowledgePoints";

	public final static String TODO_QUALITY_TOO_MANY     = "impossible.tooManyQualities";
	public final static String TODO_QUALITY_KARMAGAIN    = "impossible.gainedMoreThan20Karma";
	public final static String TODO_QUALITY_KARMASURGE   = "impossible.spentMoreThan30SURGE";

	public final static String TODO_SPELLS_TOO_MANY     = "impossible.tooManySpells";
	public final static String TODO_CFORMS_TOO_MANY     = "impossible.tooManyComplexForms";
	// Cannot bind more foci than MAGIC
	public final static String IMPOSS_TOO_MANY_FOCI      = "impossible.cannotBindMoreFoci";
	// Force of focus may not be higher than MAGIC
	public final static String IMPOSS_FOCUS_EXCEEDS_MAGIC= "impossible.focusExceedsMagic";
	// Sum of force of all foci exceeds MAGICx5
	public final static String IMPOSS_SUM_FORCE_EXCEEDS_MAX= "impossible.sumForceExceedsMax";
	// Cannot bind more foci than MAGIC
	public final static String IMPOSS_TOO_MANY_DATASTRCTURES  = "impossible.cannotBindMoreDataStructures";
	// Force of focus may not be higher than MAGIC
	public final static String IMPOSS_RATING_EXCEEDS_RESONANCE= "impossible.ratingExceedsResonance";
	// Sum of force of all foci exceeds MAGICx5
	public final static String IMPOSS_SUM_RATINGS_EXCEEDS_MAX= "impossible.sumRatingsExceedsMax";

	public static final String TODO_BORN_QUALITY_MISSING = "todo.lifepath.born.quality.missing";
	public static final String TODO_LANGUAGE_NOT_SET     = "todo.lifepath.languageNotSet";
	public static final String TODO_CHILD_QUALITY_MISSING = "todo.lifepath.child.quality.missing";
	public static final String TODO_CHILDAREA_NOT_SET     = "todo.lifepath.childAreaNotSet";
	public static final String TODO_EARLY_SKILL_MISSING = "todo.lifepath.early.skill.missing";
	public static final String TODO_EARLY_ATTRIBUTE_MISSING = "todo.lifepath.early.attribute.missing";
	public static final String TODO_NOT_ENOUGH_LIFEPATH_MODULES = "todo.lifepath.modules.notEnough";
	public static final String TODO_TOO_MANY_LIFEPATH_MODULES = "todo.lifepath.modules.tooMany";
	public static final String TODO_LIFEPATH_SELMOD_MISSING = "todo.lifepath.modules.selmodMissing";

	public static final String TODO_SHIFTER_MUST_HAVE_MAGIC = "todo.shifter_must_have_magic";
	public static final String TODO_SHIFTER_MUST_BE_METAHUMAN = "todo.shifter_must_be_metahuman";

}
