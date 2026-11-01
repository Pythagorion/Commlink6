package de.rpgframework.shadowrun6.proc;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import de.rpgframework.character.ProcessingStep;
import de.rpgframework.genericrpg.ValueType;
import de.rpgframework.genericrpg.chargen.OperationResult;
import de.rpgframework.genericrpg.data.ApplyTo;
import de.rpgframework.genericrpg.data.AttributeValue;
import de.rpgframework.genericrpg.data.DataItem;
import de.rpgframework.genericrpg.data.Decision;
import de.rpgframework.genericrpg.data.IReferenceResolver;
import de.rpgframework.genericrpg.data.SkillSpecialization;
import de.rpgframework.genericrpg.items.CarriedItem;
import de.rpgframework.genericrpg.items.CarryMode;
import de.rpgframework.genericrpg.modification.AllowModification;
import de.rpgframework.genericrpg.modification.CheckModification;
import de.rpgframework.genericrpg.modification.DataItemModification;
import de.rpgframework.genericrpg.modification.EmbedModification;
import de.rpgframework.genericrpg.modification.Modification;
import de.rpgframework.genericrpg.modification.Modification.Origin;
import de.rpgframework.genericrpg.modification.ValueModification;
import de.rpgframework.shadowrun.AdeptPower;
import de.rpgframework.shadowrun.AdeptPowerValue;
import de.rpgframework.shadowrun.BodyForm;
import de.rpgframework.shadowrun.BodyType;
import de.rpgframework.shadowrun.CritterPower;
import de.rpgframework.shadowrun.CritterPowerValue;
import de.rpgframework.shadowrun.LicenseValue;
import de.rpgframework.shadowrun.LifestyleQuality;
import de.rpgframework.shadowrun.MagicOrResonanceType;
import de.rpgframework.shadowrun.MetamagicOrEcho;
import de.rpgframework.shadowrun.MetamagicOrEchoValue;
import de.rpgframework.shadowrun.Movement;
import de.rpgframework.shadowrun.Quality;
import de.rpgframework.shadowrun.Quality.QualityType;
import de.rpgframework.shadowrun.QualityValue;
import de.rpgframework.shadowrun.SIN;
import de.rpgframework.shadowrun.SIN.FakeRating;
import de.rpgframework.shadowrun.ShadowrunAttribute;
import de.rpgframework.shadowrun.SpellValue;
import de.rpgframework.shadowrun.persist.MovementConverter;
import de.rpgframework.shadowrun6.SR6Lifestyle;
import de.rpgframework.shadowrun6.SR6Quality;
import de.rpgframework.shadowrun6.SR6RuleFlag;
import de.rpgframework.shadowrun6.SR6Skill;
import de.rpgframework.shadowrun6.SR6SkillValue;
import de.rpgframework.shadowrun6.SR6Spell;
import de.rpgframework.shadowrun6.Shadowrun6Character;
import de.rpgframework.shadowrun6.Shadowrun6Core;
import de.rpgframework.shadowrun6.Shadowrun6Tools;
import de.rpgframework.shadowrun6.items.AvailableSlot;
import de.rpgframework.shadowrun6.items.ItemTemplate;
import de.rpgframework.shadowrun6.items.SR6GearTool;
import de.rpgframework.shadowrun6.items.SR6PieceOfGearVariant;
import de.rpgframework.shadowrun6.modifications.ShadowrunReference;

/**
 * @author prelle
 *
 */
public class ApplyModificationsGeneric implements ProcessingStep {

	private final static Logger logger = System.getLogger(ApplyModificationsGeneric.class.getPackageName());

	private Shadowrun6Character model;

	//-------------------------------------------------------------------
	/**
	 */
	public ApplyModificationsGeneric(Shadowrun6Character model) {
		this.model = model;
	}

	//-------------------------------------------------------------------
	/**
	 * @return TRUE when modification was processed
	 */
	public static boolean applyModification(Shadowrun6Character model, Modification tmp) {
		try {
			if (tmp instanceof DataItemModification) {
				DataItemModification mod = (DataItemModification)tmp;
				switch ((ShadowrunReference) tmp.getReferenceType()) {
				case ADEPT_POWER: return applyAdeptPower(model, mod);
				case ATTRIBUTE  : return applyAttribute(model, (ValueModification) mod);
				case CRITTER_POWER: return applyCritterPower(model, mod);
				case GEAR       : return applyGear(model, mod);
				case LICENSE    : return applyLicense(model, mod);
				case LIFESTYLE  : return applyLifestyle(model, mod);
				case METAECHO   : return applyMetaEcho(model, mod);
				case MAGIC_RESO : return applyMagicOrResonance(model, mod);
				case QUALITY    : return applyQuality(model, mod);
				case RULE       : return applyRule(model, mod);
				case SIN        : return applySIN(model, mod);
				case SKILL		:
					if (mod instanceof ValueModification)
						return applySkill(model, (ValueModification) mod);
					else
						return applySkill(model, mod);
				case SKILLSPECIALIZATION:
					if (mod instanceof ValueModification)
						return applySkillSpec(model, (ValueModification) mod);
					else
						return applySkillSpec(model, mod);
				case SPELL		:
					return applySpell(model, mod);
				case ITEM_ATTRIBUTE:
				case ACTION:
					logger.log(Level.INFO, "Add global item {1} modification {0}", mod, mod.getOrigin());
					model.addItemModification(mod); return true;
				default:
					logger.log(Level.WARNING, "Don't know how to apply "+tmp.getReferenceType()+" of "+tmp);
					System.err.println("ApplyModificationsGeneric: Don't know how to apply "+tmp.getReferenceType()+" of "+tmp);
				}
			} else
				System.err.println("ApplyModGeneric: "+tmp.getClass());
		} catch (Exception e) {
			logger.log(Level.ERROR, "Error applying "+tmp+" from "+tmp.getSource(),e);
		}
		return false;
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.character.ProcessingStep#process(java.util.List)
	 */
	@Override
	public List<Modification> process(List<Modification> previous) {
		if (logger.isLoggable(Level.TRACE)) logger.log(Level.TRACE, "ENTER process");
		List<Modification> unprocessed = new ArrayList<>();

		try {
			// Walk modifications for creation points
			for (Modification tmp : previous) {
				logger.log(Level.DEBUG, "process {0} / {1}",tmp,tmp.getApplyTo());

				if (tmp.getSource()!=null && model.isForbiddenSource(tmp.getSource())) {
					logger.log(Level.WARNING, "Ignore {0} from forbidden source {1}", tmp, tmp.getSource());
					continue;
				}

				if (tmp instanceof AllowModification) {
					unprocessed.add(tmp);
				} else if (tmp.getApplyTo()==ApplyTo.CHARACTER || tmp.getApplyTo()==ApplyTo.UNARMED || tmp.getApplyTo()==ApplyTo.MELEE
						|| tmp.getReferenceType()==ShadowrunReference.ADEPT_POWER
						|| tmp.getReferenceType()==ShadowrunReference.ATTRIBUTE
						|| tmp.getReferenceType()==ShadowrunReference.CRITTER_POWER
						|| tmp.getReferenceType()==ShadowrunReference.GEAR
						|| tmp.getReferenceType()==ShadowrunReference.LICENSE
						|| tmp.getReferenceType()==ShadowrunReference.LIFESTYLE
						|| tmp.getReferenceType()==ShadowrunReference.MAGIC_RESO
						|| tmp.getReferenceType()==ShadowrunReference.GEAR
						|| tmp.getReferenceType()==ShadowrunReference.QUALITY
						|| tmp.getReferenceType()==ShadowrunReference.RULE
						|| tmp.getReferenceType()==ShadowrunReference.SIN
						|| tmp.getReferenceType()==ShadowrunReference.SKILL
						|| tmp.getReferenceType()==ShadowrunReference.SPELL
						) {
					try {
						MagicOrResonanceType oldMoR = model.getMagicOrResonanceType();
						if (!applyModification(model, tmp)) {
							unprocessed.add(tmp);
						} else if (tmp.getReferenceType()==ShadowrunReference.MAGIC_RESO && model.getMagicOrResonanceType()!=oldMoR) {
							logger.log(Level.WARNING, "TODO: Reapply modifications from magic/reso: {0} ",model.getMagicOrResonanceType().getOutgoingModifications());
							System.err.println("TODO: Reapply modifications from magic/reso "+model.getMagicOrResonanceType().getOutgoingModifications());
							unprocessed.addAll(model.getMagicOrResonanceType().getOutgoingModifications());
						}
					} catch (Exception e) {
						logger.log(Level.ERROR, "Error applying "+tmp+" from "+tmp.getSource(),e);
					}
				} else {
					logger.log(Level.WARNING, "Unprocessed "+tmp);
					unprocessed.add(tmp);
				}
			}
			return unprocessed;
		} finally {
			if (logger.isLoggable(Level.TRACE)) logger.log(Level.TRACE, "LEAVE process");
		}
	}

	// -------------------------------------------------------------------
	private static boolean applyAdeptPower(Shadowrun6Character model, DataItemModification mod) {
		AdeptPower item = Shadowrun6Core.getItem(AdeptPower.class, mod.getKey());
		if (item == null) {
			logger.log(Level.ERROR, "Cannot apply modification " + mod + " - no such adept power {0}", mod.getKey());
			return false;
		}
		AdeptPowerValue value = model.getAdeptPower(mod.getKey());
		logger.log(Level.ERROR, "from "+mod.getSource()+" applyAdeptPower "+item+" that currently has "+value);
		// Find an adept power that matches ID
		// AND decisions
		value = Shadowrun6Tools.getMatchIncludingDecisions(model.getAdeptPowers(),mod.getKey(), mod.getDecisions());

		if (value == null) {
			value = new AdeptPowerValue(item, 0);
			value.setInjectedBy(mod.getSource());
			// For non-rating adept powers, assume rating 1
//			if (!(mod instanceof ValueModification) && item.hasLevel()) {
//				value.setDistributed(1);
//			}
			// Handle decisions
			for (Decision dec : mod.getDecisions()) {
				value.addDecision(dec);
				logger.log(Level.DEBUG, "Add decision {0} to adept power {1}", dec, item);
			}

			model.addAutoAdeptPower(value);
			logger.log(Level.DEBUG, "Add adept power {0} to character", item);
		}
		// Mark as auto-added
		value.addIncomingModification(mod);

		if (item.hasLevel()) {
			logger.log(Level.WARNING, " Level is now distr={0}   mod={1} = " + value.getNameWithoutRating(), value.getDistributed(),
					value.getModifier());
			logger.log(Level.INFO, "  result=" + value);
		}
		return true;
	}

	// -------------------------------------------------------------------
	private static boolean applyAttribute(Shadowrun6Character model, ValueModification mod) {
		ShadowrunAttribute item = null;
		if ("CHOICE".equals(mod.getKey())) {
			UUID uuid = mod.getConnectedChoice();
			Decision dec = mod.getDecision(uuid);
			item = mod.getReferenceType().resolve(dec.getValue());
		} else {
			item = mod.getReferenceType().resolve(mod.getKey());
		}
		if (item == null) {
			logger.log(Level.ERROR, "Cannot apply modification " + mod + " - no such attribute {0}", mod.getKey());
		}
		AttributeValue<ShadowrunAttribute> value = model.getAttribute(item);
		if (mod.getApplyTo()==ApplyTo.DRAKE) {
			BodyForm drake = model.getBodyForm(BodyType.DRAKE);
			if (drake==null) {
				logger.log(Level.ERROR, "Should apply {0} to drake body, but there is no such body");
				return false;
			}
			value = drake.getAttributeValue(item);

		}
		if (mod.getSet()==ValueType.MAX)
			return false;

		if (item==ShadowrunAttribute.MOVEMENT) {
			BodyForm body = model.getBodyForms().get(0);
			Movement mov = MovementConverter.convert(mod.getRawValue());
			body.addMovement(mov);
			return true;
		}

		value.addIncomingModification(mod);
		if (!(mod instanceof CheckModification)) {
			if (mod.isDouble()) {
				logger.log(Level.DEBUG, "Added {0} to attribute {1} ({2}) from {3}", mod.getValueAsDouble(), item, mod.getSet(), mod.getSource());
			} else
				logger.log(Level.DEBUG, "Added {0} to attribute {1} ({2}) from {3}", mod.getValue(), item, mod.getSet(), mod.getSource());
		}

		return true;
	}

	// -------------------------------------------------------------------
	private static boolean applySkill(Shadowrun6Character model, ValueModification mod) {
		SR6Skill item = mod.getReferenceType().resolve(mod.getKey());
		if (item == null) {
			logger.log(Level.ERROR, "Cannot apply modification " + mod + " from {1} - no such skill {0}", mod.getKey(),mod.getSource());
		}
		SR6SkillValue value = model.getSkillValue(item);
		if (value==null  && mod.getSet()==ValueType.NATURAL) {
			value = new SR6SkillValue(item, mod.getValue());
			model.addSkillValue(value);
		}
		if (value == null) {
			if ("language".equals(item.getId()) || "knowledge".equals(item.getId())) {
				value = new SR6SkillValue(item, 0);
				model.addSkillValue(value);
				for (Decision dec : mod.getDecisions()) {
					value.addDecision(dec);
					logger.log(Level.DEBUG, "Add decision {0} to skill {1}", dec, item);
				}
			} else {
				logger.log(Level.WARNING, "applySkill for skill unset: "+mod.getKey());
				return false;
			}
		}

		value.addIncomingModification(mod);
		logger.log(Level.INFO, "Added {0} to skill {1} ({2}) from {3}", mod.getValue(), item, mod.getSet(), mod.getSource());

		return true;
	}

	// -------------------------------------------------------------------
	private static boolean applySkill(Shadowrun6Character model, DataItemModification mod) {
		SR6Skill item = mod.getReferenceType().resolve(mod.getKey());
		// Before adding a new skill, check if it already exists
		if (mod.getId()!=null) {
			SR6SkillValue value = model.getSkillValue(mod.getId());
			if (value!=null)
				return true;
		}

		SR6SkillValue value = new SR6SkillValue(item, 1);
		if (mod.getId()!=null)
			value.setUuid(mod.getId());
		value.addIncomingModification(mod);
		logger.log(Level.ERROR, "Added skill {0} (from {1})",  item, mod.getSource());
		return true;
	}

	// -------------------------------------------------------------------
	private static boolean applySkillSpec(Shadowrun6Character model, ValueModification mod) {
		SkillSpecialization<SR6Skill> item = mod.getReferenceType().resolve(mod.getKey());
		if (item == null) {
			logger.log(Level.ERROR, "Cannot apply modification " + mod + " from {1} - no such skill spec {0}", mod.getKey(),mod.getSource());
		}
//		SR6SkillValue value = model.getSkillValue(item);
//		if (value==null  && mod.getSet()==ValueType.NATURAL) {
//			value = new SR6SkillValue(item, mod.getValue());
//			model.addSkillValue(value);
//		}
//		if (value == null) {
//			if ("language".equals(item.getId()) || "knowledge".equals(item.getId())) {
//				value = new SR6SkillValue(item, 0);
//				model.addSkillValue(value);
//				for (Decision dec : mod.getDecisions()) {
//					value.addDecision(dec);
//					logger.log(Level.DEBUG, "Add decision {0} to skill {1}", dec, item);
//				}
//			} else {
//				logger.log(Level.WARNING, "applySkill for skill unset: "+mod.getKey());
//				return false;
//			}
//		}
//
//		value.addIncomingModification(mod);
//		logger.log(Level.INFO, "Added {0} to skill {1} ({2}) from {3}", mod.getValue(), item, mod.getSet(), mod.getSource());

		return true;
	}

	// -------------------------------------------------------------------
	private static boolean applySkillSpec(Shadowrun6Character model, DataItemModification mod) {
		SkillSpecialization<SR6Skill> item = mod.getReferenceType().resolve(mod.getKey());
		// Before adding a new skill, check if it already exists
//		if (mod.getId()!=null) {
//			SR6SkillValue value = model.getSkillValue(mod.getId());
//			if (value!=null)
//				return true;
//		}
//
//		SR6SkillValue value = new SR6SkillValue(item, 1);
//		if (mod.getId()!=null)
//			value.setUuid(mod.getId());
//		value.addIncomingModification(mod);
//		logger.log(Level.ERROR, "Added skill {0} (from {1})",  item, mod.getSource());
		return true;
	}

	// -------------------------------------------------------------------
	private static boolean applyGear(Shadowrun6Character model, DataItemModification mod) {
		ItemTemplate item = Shadowrun6Core.getItem(ItemTemplate.class, mod.getKey());
		// Check if item is a geardef
		if (item==null) {
			item = model.getGearDefinition(mod.getKey());
			if (item!=null) {
				logger.log(Level.DEBUG, "Resolved gear {0} from definitions in character", mod.getKey());
			}
		}
		if (item==null && mod.getSource()!=null && mod.getSource() instanceof IReferenceResolver) {
			item =((IReferenceResolver)mod.getSource()).resolveItem(mod.getKey());
			if (item!=null) {
				logger.log(Level.DEBUG, "Resolved gear {0} from definitions in data item", mod.getKey());
			}
		}
		if (item==null) {
			logger.log(Level.ERROR, "Cannot resolve gear {0} from modification {1}", mod.getKey(), mod);
			return false;
		}
		logger.log(Level.DEBUG, "applyGear {0}",mod);
		SR6PieceOfGearVariant variant = null;
		if (mod.getVariant()!=null) {
			variant = (SR6PieceOfGearVariant) item.getVariant(mod.getVariant());
		}
		CarryMode carry = CarryMode.CARRIED;
		if (!item.getUsages().isEmpty()) {
			carry = item.getUsages().get(0).getMode();
		}
		if (variant!=null && !variant.getUsages().isEmpty()) {
			carry = variant.getUsages().get(0).getMode();
		}
		Decision[] dec = mod.getDecisions().toArray(new Decision[mod.getDecisions().size()]);
		OperationResult<CarriedItem<ItemTemplate>> result = SR6GearTool.buildItem(item, carry, variant, model, false, dec);
		if (result.hasError()) {
			logger.log(Level.DEBUG, "Failed creating {0}/{1}/{2}: {3}", mod.getKey(), mod.getVariant(), carry, result.getError());
			return false;
		}
		if (mod.getId()!=null)
			result.get().setUuid(mod.getId());
		result.get().setInjectedBy(mod.getSource());
		result.get().addIncomingModification(mod);

		if (mod instanceof EmbedModification) {
			EmbedModification embed = (EmbedModification)mod;
			if (embed.getIntoId()==null) {
				logger.log(Level.ERROR, "Cannot apply EmbedModification {0} from {1} since into UUID is null", embed, embed.getSource());
				return false;
			}
			CarriedItem<ItemTemplate> target = model.getCarriedItem(embed.getIntoId());
			if (target==null) {
				logger.log(Level.WARNING, "<embed> specifies unknown item {0}", embed.getIntoId());
				return false;
			}
			AvailableSlot slot = target.getSlot(embed.getHook());
			if (slot==null) {
				logger.log(Level.WARNING, "<embed> specifies unknown slot {0} in item {1}", embed.getHook(), target);
				return false;
			}
			logger.log(Level.ERROR, "Put item {0} into slot {1} of {2}", result.get(), embed.getHook(), target);
			target.addAccessory(result.get(), embed.getHook());
			slot.addEmbeddedItem(target);
		} else {
			logger.log(Level.DEBUG, "Put item in inventory: {0}   (from {1})", result.get(), mod.getSource());
			model.addVirtualCarriedItem(result.get());
		}
		return true;
	}

	//-------------------------------------------------------------------
	private static boolean applyLifestyle(Shadowrun6Character model, DataItemModification mod) {
		LifestyleQuality item = Shadowrun6Core.getItem(LifestyleQuality.class, mod.getKey());
//		UUID uuidToSet = mod.getId();
//		if (uuidToSet==null) {
//			logger.log(Level.ERROR, "When injecting lifestyles, the modification should have an id='UUID' attribute (from {0})", mod.getSource());
//			return false;
//		}
//		SR6Lifestyle value = model.getLifestyle(uuidToSet);
//		if (value == null) {
		SR6Lifestyle
			value = new SR6Lifestyle(item);
			if (mod instanceof ValueModification) {
				value.setDistributed( ((ValueModification)mod).getValue() );
			} else
				value.setDistributed(1);
			// Handle decisions
			for (Decision dec : mod.getDecisions()) {
				value.addDecision(dec);
				logger.log(Level.DEBUG, "Add decision {0} to lifestyle {1}", dec, item);
			}
			value.setInjectedBy(mod.getSource());
			value.setUuid(UUID.randomUUID());

			logger.log(Level.WARNING, "Inject lifestyle: {0}   (from {1})", value, mod.getSource());
			model.addLifestyle(value);
			logger.log(Level.DEBUG, "Add lifestyle {0} to character", item);
//		}
		// Mark as auto-added
		value.addIncomingModification(mod);
		return true;
	}

	// -------------------------------------------------------------------
	private static boolean applyMagicOrResonance(Shadowrun6Character model, DataItemModification mod) {
		MagicOrResonanceType item = Shadowrun6Core.getItem(MagicOrResonanceType.class, mod.getKey());
		if (item == null) {
			logger.log(Level.ERROR, "Cannot apply modification " + mod + " - no such magic_reso {0}", mod.getKey());
			return false;
		}
		// Types only granted by modifications (e.g. minor_awakened) only replace mundane,
		// never a magic or resonance type chosen by the player
		MagicOrResonanceType current = model.getMagicOrResonanceType();
		if (item.isModOnly() && current!=null && !current.isMundane()) {
			logger.log(Level.INFO, "Don''t change magic/resonance type {0} to {1} from {2}", current, item, mod.getSource());
			return true;
		}

		model.setMagicOrResonanceType(item);
		logger.log(Level.INFO, "Changed magic/resonance type to {0}", item);

		return true;
	}

	// -------------------------------------------------------------------
	private static boolean applyMetaEcho(Shadowrun6Character model, DataItemModification mod) {
		MetamagicOrEcho item = Shadowrun6Core.getItem(MetamagicOrEcho.class, mod.getKey());
		if (item == null) {
			logger.log(Level.ERROR, "Cannot apply modification " + mod + " - no such metaecho {0}", mod.getKey());
			return false;
		}
		MetamagicOrEchoValue value = model.getMetamagicOrEcho(mod.getKey());
		// Find an metaecho that matches ID
		// AND decisions
		value = Shadowrun6Tools.getMatchIncludingDecisions(model.getMetamagicOrEchoes(),mod.getKey(), mod.getDecisions());

		if (value == null) {
			value = new MetamagicOrEchoValue(item);
			value.setInjectedBy(mod.getSource());
			value.addIncomingModification(mod);
			if (!(mod instanceof ValueModification) && item.hasLevel()) {
				value.setDistributed(1);
			}
			// Handle decisions
			for (Decision dec : mod.getDecisions()) {
				value.addDecision(dec);
				logger.log(Level.DEBUG, "Add decision {0} to metaecho {1}", dec, item);
			}

			model.addAutoMetamagicOrEchoe(value);
			logger.log(Level.INFO, "Add metaecho {0} to character", item);
		}
		// Mark as auto-added
		value.addIncomingModification(mod);

		return true;
	}

	//-------------------------------------------------------------------
	private static boolean applyQuality(Shadowrun6Character model, DataItemModification mod) {
		Quality item = Shadowrun6Core.getItem(SR6Quality.class, mod.getKey());
		QualityValue value = model.getQuality(mod.getKey(), mod.getId());
		if (item == null) {
			logger.log(Level.ERROR, "Cannot apply modification " + mod + " - no such quality {0}", mod.getKey());
		}
		// remove="true" - effects of the quality are suppressed in BuildForbiddenSources,
		// so here it is only important not to add the quality
		if (mod.isRemove()) {
			logger.log(Level.INFO, "Quality {0} is removed by {1}", mod.getKey(), mod.getSource());
			return true;
		}
		// Don't auto-add a quality that something else removes (e.g. orthoskin replaces the dermal deposits of trolls)
		if (value == null && item != null && model.isForbiddenSource(item)) {
			logger.log(Level.INFO, "Don't add quality {0} from {1} - it is removed by other modification", mod.getKey(), mod.getSource());
			return true;
		}

		logger.log(Level.INFO, "Add {0} with decisions {1}",mod,mod.getDecisions());
		if (value == null) {
			value = new QualityValue(item, 0);
			if (mod.getId()!=null) value.setUuid(mod.getId());
			// Handle decisions
			for (Decision dec : mod.getDecisions()) {
				if (dec.getValue().startsWith("i18n.")) {
					Decision newDec = new Decision(dec.getChoiceUUID(), dec.getValue());
					DataItem source = (DataItem)mod.getSource();
					String key = source.getTypeString()+"."+source.getId()+".choice."+dec.getValue().substring(5);
					String result = source.getLocalizedString(Locale.getDefault(), key);
					newDec.setValue(result);
					value.addDecision(newDec);
					logger.log(Level.INFO, "Add decision {0} to quality {1}", newDec, item);
				} else {
					value.addDecision(dec);
					logger.log(Level.INFO, "Add decision {0} to quality {1}", dec, item);
				}
			}

			if (item.getType()==QualityType.SHIFTER) {
				logger.log(Level.WARNING, "Add "+value+" as shifter auto");
				model.addShifterAuto(value);
			} else
				model.addQuality(value);
			logger.log(Level.INFO, "Add quality {0} to character", item);
		}
		// Mark as auto-added
		mod.setOrigin(Origin.OUTSIDE);
		value.addIncomingModification(mod);

		if (item.hasLevel()) {
			logger.log(Level.DEBUG, " Level is now distr={0}   mod={1} = " + value.getName(), value.getDistributed(),
					value.getModifier());
			logger.log(Level.DEBUG, "  result=" + value);
		}
		return true;
	}

	//-------------------------------------------------------------------
	private static boolean applyCritterPower(Shadowrun6Character model, DataItemModification mod) {
		CritterPower item = Shadowrun6Core.getItem(CritterPower.class, mod.getKey());
		CritterPowerValue value = model.getCritterPower(mod.getKey());
		if (item == null) {
			logger.log(Level.ERROR, "Cannot apply modification " + mod + " - no such quality {0}", mod.getKey());
		}
		if (value == null) {
			value = new CritterPowerValue(item, 0);
			// Handle decisions
			for (Decision dec : mod.getDecisions()) {
				value.addDecision(dec);
				logger.log(Level.DEBUG, "Add decision {0} to critter power {1}", dec, item);
			}

			if (mod.getApplyTo()==ApplyTo.DRAKE) {
				logger.log(Level.INFO, "Add critter power {0} to drake body", item);
				model.getBodyForm(BodyType.DRAKE).addCritterPower(value);

			} else {
				model.addCritterPower(value);
				logger.log(Level.INFO, "Add critter power {0} to character", item);
				return false;
			}
		}
		// Mark as auto-added
		value.addIncomingModification(mod);

		if (item.hasLevel()) {
			logger.log(Level.INFO, " Level is now distr={0}   mod={1} = " + value.getName(), value.getDistributed(),
					value.getModifier());
			logger.log(Level.DEBUG, "  result=" + value);
		}
		return true;
	}

	// -------------------------------------------------------------------
	private static boolean applyRule(Shadowrun6Character model, DataItemModification mod) {
		SR6RuleFlag item = SR6RuleFlag.valueOf(mod.getKey());
		//Rule item = Shadowrun6Rules.getRule(mod.getKey());
		if (item==null) {
			logger.log(Level.ERROR, "No such rule {0} - source {1}", mod.getKey(), mod.getSource());
			System.exit(1);
		}
		if (mod.isRemove()) {
			model.clearRuleFlag(item);
			logger.log(Level.DEBUG, "Clear rule {0} from character", item);
		} else {
			model.addRuleFlag(item);
			logger.log(Level.INFO, "Set rule {0} to character", item);
		}
		return true;
	}

	//-------------------------------------------------------------------
	private static boolean applySIN(Shadowrun6Character model, DataItemModification mod) {
		if (mod.getSource()==null)
			throw new IllegalArgumentException("No source in modification");
		int count =1;
		if (mod instanceof ValueModification) count=((ValueModification)mod).getValue();

		for (int i=0; i<count; i++) {
			SIN sin = new SIN(FakeRating.valueOf(mod.getKey()));
			sin.setName("Jane Doe");
			if (count>1)
				sin.setName("Jane Doe "+(i+1));
			sin.setInjectedBy(mod.getSource());
			if (mod.getId()!=null && count==1)
				sin.setUniqueId(mod.getId());
			logger.log(Level.INFO, "Inject SIN {0} (from {1})", sin.getUniqueId(), mod.getSource());
			model.addSIN(sin);
		}
		return true;
	}

	//-------------------------------------------------------------------
	private static boolean applyLicense(Shadowrun6Character model, DataItemModification mod) {
		if (mod.getSource()==null)
			throw new IllegalArgumentException("No source in modification");
		int count =1;
		if (mod instanceof ValueModification) count=((ValueModification)mod).getValue();

		for (int i=0; i<count; i++) {
			LicenseValue tmp = new LicenseValue("Undefined",FakeRating.valueOf(mod.getKey()));
			tmp.setName("Undefined License");
			if (count>1)
				tmp.setName("Undefined License "+(i+1));
			tmp.setInjectedBy(mod.getSource());
			if (mod.getId()!=null && count==1)
				tmp.setUniqueId(mod.getId());
			logger.log(Level.DEBUG, "Inject License {0} (from {1})", tmp.getUniqueId(), mod.getSource());
			model.addLicense(tmp);
		}
		return true;
	}

	// -------------------------------------------------------------------
	private static boolean applySpell(Shadowrun6Character model, DataItemModification mod) {
		SR6Spell item = Shadowrun6Core.getItem(SR6Spell.class, mod.getKey());
		if (item == null) {
			logger.log(Level.ERROR, "Cannot apply modification " + mod + " - no such spell {0}", mod.getKey());
			return false;
		}
		SpellValue<SR6Spell> value = model.getSpells().stream().filter(s -> s.getResolved()==item).findFirst().orElse(null);
		logger.log(Level.ERROR, "from "+mod.getSource()+" applyAdeptPower "+item+" that currently has "+value);
		// Find an adept power that matches ID
		// AND decisions
		value = Shadowrun6Tools.getMatchIncludingDecisions(model.getSpells(),mod.getKey(), mod.getDecisions());

		if (value == null) {
			value = new SpellValue<SR6Spell>(item);
			value.setInjectedBy(mod.getSource());
			// For non-rating adept powers, assume rating 1
//			if (!(mod instanceof ValueModification) && item.hasLevel()) {
//				value.setDistributed(1);
//			}
			// Handle decisions
			for (Decision dec : mod.getDecisions()) {
				value.addDecision(dec);
				logger.log(Level.DEBUG, "Add decision {0} to spell {1}", dec, item);
			}

			//model.addAutoAdeptPower(value);
			model.addSpell(value);
			logger.log(Level.DEBUG, "Add spell {0} to character", item);
		}
		// Mark as auto-added
		value.addIncomingModification(mod);
		return true;
	}

}
