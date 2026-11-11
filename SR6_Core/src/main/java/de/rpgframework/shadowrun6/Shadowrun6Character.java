package de.rpgframework.shadowrun6;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.prelle.simplepersist.Element;
import org.prelle.simplepersist.ElementList;
import org.prelle.simplepersist.Root;

import de.rpgframework.character.RuleSpecificCharacterObject;
import de.rpgframework.classification.Gender;
import de.rpgframework.core.RoleplayingSystem;
import de.rpgframework.genericrpg.SetItemValue;
import de.rpgframework.genericrpg.data.AttributeValue;
import de.rpgframework.genericrpg.items.CarriedItem;
import de.rpgframework.genericrpg.modification.CheckModification;
import de.rpgframework.genericrpg.modification.RelevanceModification;
import de.rpgframework.genericrpg.modification.ValueModification;
import de.rpgframework.shadowrun.MagicOrResonanceType;
import de.rpgframework.shadowrun.QualityValue;
import de.rpgframework.shadowrun.ShadowrunAction;
import de.rpgframework.shadowrun.ShadowrunAttribute;
import de.rpgframework.shadowrun.ShadowrunCharacter;
import de.rpgframework.shadowrun.SkillType;
import de.rpgframework.shadowrun.Tradition;
import de.rpgframework.shadowrun6.filter.CarriedItemItemTypeFilter;
import de.rpgframework.shadowrun6.items.ItemTemplate;
import de.rpgframework.shadowrun6.items.ItemType;
import de.rpgframework.shadowrun6.items.SR6ItemAttribute;
import de.rpgframework.shadowrun6.modifications.ShadowrunCheckInfluence;
import de.rpgframework.shadowrun6.modifications.ShadowrunReference;

/**
 * @author prelle
 *
 */
@Root(name="sr6char")
public class Shadowrun6Character extends ShadowrunCharacter<SR6Skill, SR6SkillValue, ItemTemplate, SR6Spell> implements RuleSpecificCharacterObject<ShadowrunAttribute, SR6Skill, SR6SkillValue, ItemTemplate> {

	@Element
	@Deprecated
	private PowerLevel powerLevel;
	@ElementList(entry="lifestyle", type = SR6Lifestyle.class, inline = false)
	private List<SR6Lifestyle> lifestyles;
	@ElementList(entry="qpath", type = QualityPathValue.class, inline = false)
	private List<QualityPathValue> qpaths;
	@ElementList(entry="martialartref", type=MartialArtsValue.class)
	protected List<MartialArtsValue> martialArts;
	@ElementList(entry="techniqueref", type=TechniqueValue.class)
	protected List<TechniqueValue> techniques;
	@ElementList(entry="maneuvers", type=SignatureManeuver.class)
	protected List<SignatureManeuver> maneuvers;
	@Element
	private ASDFMapping asdfMap;
	@Element
	private DrakeTypeValue drakeType;
	@Element(name="collective")
	private SetItemValue surgeCollective;
	@ElementList(entry="datastructure", type=DataStructureValue.class)
	protected List<DataStructureValue> datastructures;
	@ElementList(entry="valmod", type=ValueModification.class)
	protected List<ValueModification> essenceChanges;
	@Element
	protected Integer essenceLossZero;
	@ElementList(entry="quality", type = QualityValue.class, inline = false)
	private List<QualityValue> shifterAddons;
	@Element
	private String shifterAnimal;
	@Element
	private String lifepathBabyBackground;
	@Element
	private String lifepathChildhoodBackground;
	@Element
	private String lifepathComingOfAgeBackground;
	@Element
	private String lifepathAdultBackground;
	@Element
	private String backgroundNotes;
	private transient List<QualityValue> shifterAuto;

	protected transient List<CheckModification> edgeMods;
	protected transient List<RelevanceModification> relevanceMods;

	private transient Persona persona;

	//-------------------------------------------------------------------
	public Shadowrun6Character() {
		gender = Gender.MALE;
		lifestyles = new ArrayList<>();
		qpaths = new ArrayList<>();
		relevanceMods = new ArrayList<>();
		edgeMods  = new ArrayList<>();
		techniques = new ArrayList<>();
		martialArts = new ArrayList<>();
		maneuvers = new ArrayList<>();
		datastructures = new ArrayList<>();
		asdfMap = new ASDFMapping();
		essenceChanges = new ArrayList<>();
		shifterAuto    = new ArrayList<>();
		shifterAddons  = new ArrayList<>();

		for (ShadowrunAttribute key : ShadowrunAttribute.primaryValuesPlusEdge()) {
			setAttribute(new AttributeValue<ShadowrunAttribute>(key, 1));
		}
		setAttribute(new AttributeValue<ShadowrunAttribute>(ShadowrunAttribute.MAGIC, 0));
		setAttribute(new AttributeValue<ShadowrunAttribute>(ShadowrunAttribute.RESONANCE, 0));
		setAttribute(new AttributeValue<ShadowrunAttribute>(ShadowrunAttribute.ESSENCE_HOLE, 0));
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.shadowrun.ShadowrunCharacter#getAspectSkill()
	 */
	@Override
	public SR6Skill getAspectSkill() {
		if (this.getAspectSkillId()==null) return null;
		return Shadowrun6Core.getSkill(getAspectSkillId());
	}

	//-------------------------------------------------------------------
	public SR6SkillValue addSkillValue(SR6SkillValue value) {
		// You can have multiple times knowledge or language
		// but other skills are unique
		SkillType type = value.getModifyable().getType();
		if (skills.contains(value) && !(type==SkillType.KNOWLEDGE || type==SkillType.LANGUAGE)) {
			throw new RuntimeException("Hab ich schon");
			//return value;
		}

		return super.addSkillValue(value);
//		skills.add(value);
//		return value;
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.character.RuleSpecificCharacterObject#getRules()
	 */
	@Override
	public RoleplayingSystem getRules() {
		return RoleplayingSystem.SHADOWRUN6;
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.shadowrun.ShadowrunCharacter#getMetatype()
	 */
	@SuppressWarnings("unchecked")
	public SR6MetaType getMetatype() {
		return Shadowrun6Core.getItem(SR6MetaType.class, metatype, Locale.getDefault().getLanguage());
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.character.RuleSpecificCharacterObject#getShortDescription()
	 */
	@Override
	public String getShortDescription() {
		List<String> names = new ArrayList<>();
		SR6MetaType meta = getMetatype();
		if (meta!=null) {
			names.add(meta.getName(Locale.getDefault()));
		}
		if (getMagicOrResonanceType()!=null) {
			names.add(getMagicOrResonanceType().getName(Locale.getDefault()));
		}
		if (gender!=null) {
			names.add(gender.getName(Locale.getDefault()));
		}
		return String.join(", ", names);
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.shadowrun.ShadowrunCharacter#getMagicOrResonanceType()
	 */
	@Override
	public MagicOrResonanceType getMagicOrResonanceType() {
		return Shadowrun6Core.getItem(MagicOrResonanceType.class, magicOrResonance);
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.shadowrun.ShadowrunCharacter#getTradition()
	 */
	@Override
	public Tradition getTradition() {
		return Shadowrun6Core.getItem(Tradition.class, tradition);
	}

//	//-------------------------------------------------------------------
//	/**
//	 * @return the powerLevel
//	 */
//	public PowerLevel getPowerLevel() {
//		return powerLevel;
//	}
//
//	//-------------------------------------------------------------------
//	/**
//	 * @param powerLevel the powerLevel to set
//	 */
//	public void setPowerLevel(PowerLevel powerLevel) {
//		this.powerLevel = powerLevel;
//	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.shadowrun.ShadowrunCharacter#getLifestyles()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<SR6Lifestyle> getLifestyles() {
		return new ArrayList<>(lifestyles);
	}

	//-------------------------------------------------------------------
	public SR6Lifestyle getLifestyle(UUID uuid) {
		Optional<SR6Lifestyle> opt = lifestyles.stream().filter(ls -> uuid.equals(ls.getUuid())).findFirst();
		if (opt.isPresent())
			return opt.get();
		return null;
	}

	//-------------------------------------------------------------------
	public void addLifestyle(SR6Lifestyle value) {
		if (!lifestyles.contains(value))
			lifestyles.add(value);
	}

	//-------------------------------------------------------------------
	public void removeLifestyle(SR6Lifestyle value) {
		lifestyles.remove(value);
	}

	//-------------------------------------------------------------------
	public List<CarriedItem<ItemTemplate>> getCarriedItems(ItemType... types) {
		CarriedItemItemTypeFilter filter = new CarriedItemItemTypeFilter(null, types);
		
		List<CarriedItem<ItemTemplate>> ret = new ArrayList<>();
		for (CarriedItem<ItemTemplate> item : getCarriedItems()) {
			ret.add(item);
			item.getAlternates().forEach(i -> ret.add((CarriedItem<ItemTemplate>) i));
		}
		return ret.stream()
			.filter(filter)
			.collect(Collectors.toList())
			;
	}

	//-------------------------------------------------------------------
	public Persona getPersona() {
		return persona;
	}

	//-------------------------------------------------------------------
	/**
	 * @param persona the persona to set
	 */
	public void setPersona(Persona persona) {
		this.persona = persona;
	}

	//-------------------------------------------------------------------
	public SR6ItemAttribute getMatrixAttributeMapping(SR6ItemAttribute value) {
		if (asdfMap!=null) {
			switch (value) {
			case ATTACK: return (asdfMap.getAttack()!=null)?asdfMap.getAttack():value;
			case SLEAZE: return (asdfMap.getSleaze()!=null)?asdfMap.getSleaze():value;
			case DATA_PROCESSING: return (asdfMap.getDataProcessing()!=null)?asdfMap.getDataProcessing():value;
			case FIREWALL: return (asdfMap.getFirewall()!=null)?asdfMap.getFirewall():value;
			default:
			}
		}
		return value;
	}

	//-------------------------------------------------------------------
	public List<QualityPathValue> getQualityPaths() {
		return qpaths;
	}

	//-------------------------------------------------------------------
	public void addQualityPath(QualityPathValue value) {
		if (!qpaths.contains(value))
			qpaths.add(value);
	}

	//-------------------------------------------------------------------
	public void removeQualityPath(QualityPathValue value) {
		qpaths.remove(value);
	}

	//-------------------------------------------------------------------
	public void clearRelevanceModifications() {
		relevanceMods.clear();
	}

	//-------------------------------------------------------------------
	public void addRelevanceModification(RelevanceModification mod) {
		relevanceMods.add(mod);
	}

	//-------------------------------------------------------------------
	public List<RelevanceModification> getRelevanceModifications(RelevanceType type) {
		return relevanceMods.stream().filter( mod -> (mod.getType().equals(type.name()))).collect(Collectors.toList());
	}

	//-------------------------------------------------------------------
	public void clearEdgeModifications() {
		edgeMods.clear();
	}

	//-------------------------------------------------------------------
	public void addEdgeModification(CheckModification mod) {
		edgeMods.add(mod);
	}

	//-------------------------------------------------------------------
	public List<CheckModification> getEdgeModifications(ShadowrunCheckInfluence... types) {
		return edgeMods.stream().filter( mod -> List.of(types).contains(mod.getWhat())).collect(Collectors.toList());
	}

	//---------------------------------------------------------
	public int getCostOfAction(ShadowrunAction action) {
		int cost = action.getCost();
		for (CheckModification mod : edgeMods) {
			if (mod.getReferenceType()==ShadowrunReference.ACTION && mod.getResolvedKey()==action && mod.getWhat()==ShadowrunCheckInfluence.EDGE_BOOST) {
				if (mod.getValue()<0)
					cost += mod.getValue();
				else
					cost = mod.getValue();
			}
		}
		if (cost<1)
			cost=1;
		return cost;
	}

	//---------------------------------------------------------
	public boolean isCostModified(ShadowrunAction action) {
		for (CheckModification mod : edgeMods) {
			if (mod.getReferenceType()==ShadowrunReference.ACTION && mod.getResolvedKey()==action && mod.getWhat()==ShadowrunCheckInfluence.EDGE_BOOST) {
				return true;
			}
		}
		return false;
	}

	//-------------------------------------------------------------------
	public List<MartialArtsValue> getMartialArts() {
		ArrayList<MartialArtsValue> ret = new ArrayList<>(martialArts);
		return ret;
	}

	//-------------------------------------------------------------------
	public void addMartialArt(MartialArtsValue ref) {
		if (!martialArts.contains(ref))
			martialArts.add(ref);
	}

	//-------------------------------------------------------------------
	public void removeMartialArt(MartialArtsValue ref) {
		martialArts.remove(ref);
		// Remove all techniques belonging to that martial art
		for (TechniqueValue tech : getTechniques()) {
			if (tech.getMartialArt()==ref.getResolved())
				removeTechnique(tech);
		}
	}

	//-------------------------------------------------------------------
	public boolean hasTechnique(Technique ref) {
		for (TechniqueValue tech : techniques) {
			if (ref==tech.getResolved())
				return true;
		}
		// Is it a key technique of a style?
		for (MartialArtsValue style : martialArts) {
			if (style.getResolved().getSignatureTechnique()==ref)
				return true;
		}

		return false;
	}

	//-------------------------------------------------------------------
	public List<TechniqueValue> getTechniques() {
		ArrayList<TechniqueValue> ret = new ArrayList<>(techniques);
		return ret;
	}

	//-------------------------------------------------------------------
	public List<TechniqueValue> getTechniquesAll() {
		ArrayList<TechniqueValue> ret = new ArrayList<>(techniques);
		// Add signature techniques
		for (MartialArtsValue style : martialArts) {
			ret.add(new TechniqueValue(style.getResolved().getSignatureTechnique(), style.getResolved()));
		}

		return ret;
	}

	//-------------------------------------------------------------------
	public List<TechniqueValue> getTechniques(MartialArts learnedIn) {
		ArrayList<TechniqueValue> ret = new ArrayList<>();
		ret.add(new TechniqueValue(learnedIn.getSignatureTechnique(), learnedIn));
		for (TechniqueValue tech : techniques) {
			if (tech.getMartialArt()==learnedIn)
				ret.add(tech);
		}

		return ret;
	}

	//-------------------------------------------------------------------
	public void addTechnique(TechniqueValue ref) {
		if (!techniques.contains(ref))
			techniques.add(ref);
	}

	//-------------------------------------------------------------------
	public void removeTechnique(TechniqueValue ref) {
		techniques.remove(ref);
	}

	//-------------------------------------------------------------------
	public void addSignatureManeuver(SignatureManeuver ref) {
		if (!maneuvers.contains(ref))
			maneuvers.add(ref);
	}

	//-------------------------------------------------------------------
	public void removeSignatureManeuver(SignatureManeuver ref) {
		maneuvers.remove(ref);
	}

	//-------------------------------------------------------------------
	public List<SignatureManeuver> getSignatureManeuvers() {
		return new ArrayList<SignatureManeuver>(maneuvers);
	}

	//-------------------------------------------------------------------
	public ASDFMapping getMatrixAttribMap() {
		if (asdfMap==null) {
			asdfMap = new ASDFMapping();
		}
		return asdfMap;
	}

	//-------------------------------------------------------------------
	public DrakeTypeValue getDrakeType() {
		return drakeType;
	}

	//-------------------------------------------------------------------
	public void setDrakeType(DrakeTypeValue value) {
		this.drakeType = value;
	}

	//-------------------------------------------------------------------
	/**
	 * @return the surgeCollective
	 */
	public SetItemValue getSurgeCollective() {
		return surgeCollective;
	}

	//-------------------------------------------------------------------
	/**
	 * @param surgeCollective the surgeCollective to set
	 */
	public void setSurgeCollective(SetItemValue data) {
		if (data!=null && !"general".equals(data.getKey()))
			this.surgeCollective = data;
		else
			this.surgeCollective = null;
	}

	//-------------------------------------------------------------------
	public List<DataStructureValue> getDataStructures() {
		return datastructures;
	}

	//-------------------------------------------------------------------
	public void addDataStructure(DataStructureValue value) {
		if (!datastructures.contains(value))
			datastructures.add(value);
	}

	//-------------------------------------------------------------------
	public void removeDataStructure(DataStructureValue value) {
		datastructures.remove(value);
	}

	//-------------------------------------------------------------------
	public CarriedItem<ItemTemplate> getSoftwareLibrary() {
		return getCarriedItem(ItemTemplate.UUID_UNUSED_SOFTWARE_DEVICE);
	}

	//-------------------------------------------------------------------
	public List<ValueModification> getEssenceChanges() {
		return essenceChanges;
	}

	//-------------------------------------------------------------------
	/**
	 * @return the essenceLossZero
	 */
	public Integer getEssenceLossZero() {
		return essenceLossZero;
	}

	//-------------------------------------------------------------------
	/**
	 * @param essenceLossZero the essenceLossZero to set
	 */
	public void setEssenceLossZero(Integer essenceLossZero) {
		this.essenceLossZero = essenceLossZero;
	}

	//-------------------------------------------------------------------
	/**
	 * @return the qualities
	 */
	public List<QualityValue> getShifterAddOns() {
		return (shifterAddons==null)?List.of():shifterAddons;
	}

	//-------------------------------------------------------------------
	public void addShifterAddOn(QualityValue value) {
		if (!shifterAddons.contains(value))
			shifterAddons.add(value);
	}

	//-------------------------------------------------------------------
	public void removeShifterAddOn(QualityValue value) {
		shifterAddons.remove(value);
	}

//	//-------------------------------------------------------------------
//	private void refreshAuto() {
//		if (auto.isEmpty()) {
//			for (Modification mod : base.getOutgoingModifications()) {
//				System.err.println("ShifterQuality.getQualities: "+mod);
//				if (mod instanceof DataItemModification diMod && diMod.getReferenceType()==ShadowrunReference.QUALITY) {
//					SR6Quality resolved = Shadowrun6Core.getItem(SR6Quality.class, diMod.getKey());
//					QualityValue fixed = new QualityValue(resolved,0);
//					auto.add(fixed);
//				}
//			}			
//		}
//	}

	//-------------------------------------------------------------------
	/**
	 * @return the shifterAuto
	 */
	public List<QualityValue> getShifterAuto() {
		return shifterAuto;
	}

	//-------------------------------------------------------------------
	public void clearShifterAuto() {
		shifterAuto.clear();
	}

	//-------------------------------------------------------------------
	public void addShifterAuto(QualityValue value) {
		System.getLogger("de.rpgframework.shadowrun").log(Level.WARNING, "Add "+value+" as shifter auto");
		if (shifterAuto.stream().anyMatch(x -> x.getModifyable()==value.getModifyable())) {
			System.getLogger("de.rpgframework.shadowrun").log(Level.WARNING, "No, don't add "+value+" as shifter auto");
			return;
		}
		if (!shifterAuto.contains(value))
			shifterAuto.add(value);
	}

	//-------------------------------------------------------------------
	public String getShifterAnimal() {
		return shifterAnimal;
	}

	//-------------------------------------------------------------------
	public void setShifterAnimal(String shifterAnimal) {
		this.shifterAnimal = shifterAnimal;
	}

	//-------------------------------------------------------------------
	public String getLifepathBabyBackground() {
		return lifepathBabyBackground;
	}

	//-------------------------------------------------------------------
	public void setLifepathBabyBackground(String lifepathBabyBackground) {
		this.lifepathBabyBackground = lifepathBabyBackground;
	}

	//-------------------------------------------------------------------
	public String getLifepathChildhoodBackground() {
		return lifepathChildhoodBackground;
	}

	//-------------------------------------------------------------------
	public void setLifepathChildhoodBackground(String lifepathChildhoodBackground) {
		this.lifepathChildhoodBackground = lifepathChildhoodBackground;
	}

	//-------------------------------------------------------------------
	public String getLifepathComingOfAgeBackground() {
		return lifepathComingOfAgeBackground;
	}

	//-------------------------------------------------------------------
	public void setLifepathComingOfAgeBackground(String lifepathComingOfAgeBackground) {
		this.lifepathComingOfAgeBackground = lifepathComingOfAgeBackground;
	}

	//-------------------------------------------------------------------
	public String getLifepathAdultBackground() {
		return lifepathAdultBackground;
	}

	//-------------------------------------------------------------------
	public void setLifepathAdultBackground(String lifepathAdultBackground) {
		this.lifepathAdultBackground = lifepathAdultBackground;
	}

	//-------------------------------------------------------------------
	public String getBackgroundNotes() {
		return backgroundNotes;
	}

	//-------------------------------------------------------------------
	public void setBackgroundNotes(String backgroundNotes) {
		this.backgroundNotes = backgroundNotes;
	}

}
