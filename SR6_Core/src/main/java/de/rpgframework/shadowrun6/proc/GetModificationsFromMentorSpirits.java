package de.rpgframework.shadowrun6.proc;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;

import de.rpgframework.character.ProcessingStep;
import de.rpgframework.genericrpg.data.Choice;
import de.rpgframework.genericrpg.data.Decision;
import de.rpgframework.genericrpg.data.GenericRPGTools;
import de.rpgframework.genericrpg.modification.DataItemModification;
import de.rpgframework.genericrpg.modification.Modification;
import de.rpgframework.genericrpg.modification.ModificationChoice;
import de.rpgframework.shadowrun.MagicOrResonanceType;
import de.rpgframework.shadowrun.MentorSpirit;
import de.rpgframework.shadowrun.Quality;
import de.rpgframework.shadowrun.QualityValue;
import de.rpgframework.shadowrun6.SR6RuleFlag;
import de.rpgframework.shadowrun6.Shadowrun6Character;
import de.rpgframework.shadowrun6.Shadowrun6Core;

/**
 * @author prelle
 *
 */
public class GetModificationsFromMentorSpirits implements ProcessingStep {

	protected static final Logger logger = System.getLogger(GetModificationsFromMentorSpirits.class.getPackageName()+".quality");

	private Shadowrun6Character model;

	//-------------------------------------------------------------------
	public GetModificationsFromMentorSpirits(Shadowrun6Character model) {
		this.model = model;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.shadowrun6.proc.CharacterProcessor#process(org.prelle.shadowrun5.ShadowrunCharacter, java.util.List)
	 */
	@Override
	public List<Modification> process(List<Modification> previous) {
		List<Modification> unprocessed = new ArrayList<>(previous);

		logger.log(Level.TRACE, "ENTER: process");
		try {

			for (QualityValue val : model.getQualities()) {
				if (!val.getKey().equals("mentor_spirit"))
					continue;
				Quality resolved = val.getResolved();
				Choice choice = resolved.getChoices().get(0);
				Decision dec = val.getDecision( choice.getUUID() );
				if (dec==null) {
					logger.log(Level.ERROR, "No decision made for Mentor Spirit");
				} else {
					MentorSpirit mentor = choice.getChooseFrom().resolve(dec.getValue());
					if (mentor==null) {
						logger.log(Level.ERROR, "Unknown Mentor Spirit ''{0}''", dec.getValue());
					} else {
						// Build a non-common modification list
						List<Modification> allMods = new ArrayList<>();
						MagicOrResonanceType type = model.getMagicOrResonanceType();
						boolean useMagician = type.usesSpells();
						boolean useAdept    = type.usesPowers();
						// Mystic adepts decide whether to use magician or adept advantages
						Decision advantages = val.getDecision(QualityValue.MENTOR_SPIRIT_ADVANTAGES);
						if (useMagician && useAdept && advantages!=null) {
							MagicOrResonanceType chosen = Shadowrun6Core.getItem(MagicOrResonanceType.class, advantages.getValue());
							if (chosen!=null) {
								useMagician = chosen.usesSpells();
								useAdept    = chosen.usesPowers();
							} else {
								logger.log(Level.ERROR, "Unknown mentor spirit advantages ''{0}''", advantages.getValue());
							}
						}
						// Some qualities grant both advantages
						if ((useMagician || useAdept) && model.hasRuleFlag(SR6RuleFlag.MENTOR_SPIRIT_BOTH)) {
							useMagician = true;
							useAdept    = true;
						}
						// Magician modifications
						if (useMagician) {
							allMods.addAll(mentor.getMagicianModifications());
						}
						// Adept modifications
						if (useAdept) {
							allMods.addAll(mentor.getAdeptModifications());
						}

						// Replace decided <selmod>s with the chosen option
						for (Modification tmp : new ArrayList<>(allMods)) {
							if (!(tmp instanceof ModificationChoice selMod))
								continue;
							List<Modification> chosen = GenericRPGTools.resolveModificationChoice(selMod, val);
							if (chosen!=null) {
								int idx = allMods.indexOf(selMod);
								allMods.remove(idx);
								allMods.addAll(idx, chosen);
							}
						}

						for (Modification tmp : allMods) {
							if (tmp.getReferenceType()==null) {
								unprocessed.add(tmp);
							} else {
								Modification mod = tmp.getReferenceType().instantiateModification(tmp, val, val.getModifiedValue(), model);
								mod.setSource(mentor);
//								// Copy all decisions
//								if (mod instanceof DataItemModification) {
//									((DataItemModification)mod).setDecisions(val.getDecisions());
//								}
								logger.log(Level.DEBUG, "add modification {0} from {1}",mod,mentor);
								unprocessed.add(mod);
							}
						}


					}
				}
			}
		} finally {
			logger.log(Level.TRACE, "LEAVE : process() ends with "+unprocessed.size()+" modifications still to process");
		}
		return unprocessed;
	}

}
