package de.rpgframework.shadowrun6.chargen.gen.lifepath;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.rpgframework.shadowrun6.LifepathModuleValue;
import de.rpgframework.shadowrun6.PowerLevel;
import de.rpgframework.shadowrun6.SR6Quality;
import de.rpgframework.shadowrun6.SR6Skill;
import de.rpgframework.shadowrun6.Shadowrun6Core;
import de.rpgframework.shadowrun.ShadowrunAttribute;
import de.rpgframework.shadowrun.QualityValue;
import de.rpgframework.shadowrun6.chargen.gen.CommonSR6GeneratorSettings;

/**
 * @author prelle
 *
 */
public class SR6LifePathSettings extends CommonSR6GeneratorSettings {

	private List<LifepathModuleValue> modules;

	private String nativeLanguage;
	private String bornQual1, bornQual2;
	private List<String> bornQualities;
	private List<QualityValue> surgeQualities;
	private String childhoodArea;
	private String basicSkills;
	private String childQual1, childQual2;
	private String earlySkill;
	private ShadowrunAttribute earlyAttribute;
	private String earlyQual1, earlyQual2;
	/** ID of the connection gained when coming of age */
	private String earlyContact;
	private PowerLevel lifePathDefaultsAppliedFor;

	//-------------------------------------------------------------------
	/**
	 */
	public SR6LifePathSettings() {
		modules = new ArrayList<>();
		bornQualities = new ArrayList<>();
		surgeQualities = new ArrayList<>();
	}

	//-------------------------------------------------------------------
	public List<LifepathModuleValue> getModules() {
		return modules;
	}

	//-------------------------------------------------------------------
	public void addModule(LifepathModuleValue module) {
		modules.add(module);
	}

	//-------------------------------------------------------------------
	public void removeModule(LifepathModuleValue module) {
		modules.remove(module);
	}

	//-------------------------------------------------------------------
	public String toAttributeString() {
		StringBuffer buf = new StringBuffer();
//		buf.append("\nCharacter Points remaining: "+characterPoints);
//		buf.append("\nCP converted to special attributes: "+cpBoughtSpecial);
//		buf.append("\nCP converted to regular attributes: "+cpBoughtAttrib);
//		for (Entry<ShadowrunAttribute,PerAttributePoints> ent : perAttrib.entrySet()) {
//			buf.append(String.format("\n%10s : %s", ent.getKey(), ent.getValue().toString()));
//		}
		return buf.toString();
	}

	//-------------------------------------------------------------------
	public String toSkillString() {
		StringBuffer buf = new StringBuffer();
//		buf.append("\nCharacter Points remaining: "+characterPoints);
//		buf.append("\nCP converted to skills: "+cpToSkills);
//		for (Entry<SR6SkillValue,PerSkillPoints> ent : perSkill.entrySet()) {
//			if (ent.getValue().getSum()>0)
//				buf.append(String.format("\n%10s : %s", ent.getKey().getSkill(), ent.getValue().toString()));
//		}
		return buf.toString();
	}

	//-------------------------------------------------------------------
	public SR6Quality getBornQuality1() {
		if (bornQual1 == null) return null;
		return Shadowrun6Core.getItem(SR6Quality.class, bornQual1);
	}

	//-------------------------------------------------------------------
	public void setBornQuality1(SR6Quality value) {
		if (value==null) this.bornQual1=null;
		else this.bornQual1 = value.getId();
		List<String> ids = getBornQualityIds();
		setBornQualityIdAt(0, bornQual1);
	}

	//-------------------------------------------------------------------
	public SR6Quality getBornQuality2() {
		if (bornQual2 == null) return null;
		return Shadowrun6Core.getItem(SR6Quality.class, bornQual2);
	}

	//-------------------------------------------------------------------
	public void setBornQuality2(SR6Quality value) {
		if (value==null) this.bornQual2=null;
		else this.bornQual2 = value.getId();
		setBornQualityIdAt(1, bornQual2);
	}

	//-------------------------------------------------------------------
	public List<SR6Quality> getBornQualities() {
		List<SR6Quality> ret = new ArrayList<>();
		for (String id : getBornQualityIds()) {
			SR6Quality quality = Shadowrun6Core.getItem(SR6Quality.class, id);
			if (quality!=null) ret.add(quality);
		}
		return ret;
	}

	//-------------------------------------------------------------------
	public void addBornQuality(SR6Quality value) {
		if (value==null)
			return;
		List<String> ids = getBornQualityIds();
		if (!ids.contains(value.getId()))
			ids.add(value.getId());
		syncLegacyBornQualities();
	}

	//-------------------------------------------------------------------
	public void removeBornQuality(SR6Quality value) {
		if (value==null)
			return;
		getBornQualityIds().remove(value.getId());
		syncLegacyBornQualities();
	}

	//-------------------------------------------------------------------
	private List<String> getBornQualityIds() {
		if (bornQualities==null)
			bornQualities = new ArrayList<>();
		if (bornQualities.isEmpty()) {
			if (bornQual1!=null) bornQualities.add(bornQual1);
			if (bornQual2!=null && !bornQual2.equals(bornQual1)) bornQualities.add(bornQual2);
		}
		return bornQualities;
	}

	//-------------------------------------------------------------------
	private void setBornQualityIdAt(int index, String id) {
		List<String> ids = getBornQualityIds();
		while (ids.size()<=index)
			ids.add(null);
		ids.set(index, id);
		ids.removeIf(val -> val==null);
		syncLegacyBornQualities();
	}

	//-------------------------------------------------------------------
	private void syncLegacyBornQualities() {
		List<String> ids = getBornQualityIds();
		bornQual1 = ids.size()>0?ids.get(0):null;
		bornQual2 = ids.size()>1?ids.get(1):null;
	}

	//-------------------------------------------------------------------
	public List<QualityValue> getSurgeQualities() {
		if (surgeQualities==null)
			surgeQualities = new ArrayList<>();
		return surgeQualities;
	}

	//-------------------------------------------------------------------
	public void addSurgeQuality(QualityValue value) {
		if (value==null || value.getKey()==null)
			return;
		getSurgeQualities().removeIf(existing -> value.getKey().equals(existing.getKey()));
		getSurgeQualities().add(value);
	}

	//-------------------------------------------------------------------
	public void removeSurgeQuality(QualityValue value) {
		if (value==null)
			return;
		getSurgeQualities().removeIf(existing -> value.getKey().equals(existing.getKey()));
	}

	//-------------------------------------------------------------------
	public String getNativeLanguage() {
		return nativeLanguage;
	}

	//-------------------------------------------------------------------
	public void setNativeLanguage(String value) {
		this.nativeLanguage = value;
	}

	//-------------------------------------------------------------------
	public SR6Quality getChildhoodQuality1() {
		if (childQual1 == null) return null;
		return Shadowrun6Core.getItem(SR6Quality.class, childQual1);
	}

	//-------------------------------------------------------------------
	public void setChildhoodQuality1(SR6Quality value) {
		if (value==null) this.childQual1=null;
		else this.childQual1 = value.getId();
	}

	//-------------------------------------------------------------------
	public SR6Quality getChildhoodQuality2() {
		if (childQual2 == null) return null;
		return Shadowrun6Core.getItem(SR6Quality.class, childQual2);
	}

	//-------------------------------------------------------------------
	public void setChildhoodQuality2(SR6Quality value) {
		if (value==null) this.childQual2=null;
		else this.childQual2 = value.getId();
	}

	//-------------------------------------------------------------------
	public String getChildhoodArea() {
		return childhoodArea;
	}

	//-------------------------------------------------------------------
	public void setChildhoodArea(String value) {
		this.childhoodArea = value;
	}

	//-------------------------------------------------------------------
	public List<SR6Skill> getChildhoodSkills() {
		List<SR6Skill> ret = new ArrayList<>();
		if (basicSkills!=null) {
			String[] keys = basicSkills.split(",");
			for (String key : keys) {
				SR6Skill skill = Shadowrun6Core.getItem(SR6Skill.class, key.trim());
				if (skill!=null) ret.add(skill);
			}
		}
		return ret;
	}

	//-------------------------------------------------------------------
	public void setChildhoodSkills(List<SR6Skill> keys) {
		if (keys.size()>4)
			throw new IllegalArgumentException("Too many skills selected");
		basicSkills = String.join(",", keys.stream().map(key -> key.getId()).toList());
	}

	//-------------------------------------------------------------------
	public SR6Skill getEarlyAdultSkill() {
		SR6Skill ret = null;
		if (earlySkill!=null) {
			ret = Shadowrun6Core.getItem(SR6Skill.class, earlySkill.trim());
		}
		return ret;
	}
	
	//-------------------------------------------------------------------
	public void setEarlyAdultSkill(SR6Skill skill) {
		earlySkill=(skill!=null)?skill.getId():null;
	}

	//-------------------------------------------------------------------
	public SR6Quality getEarlyAdultQuality1() {
		if (earlyQual1 == null) return null;
		return Shadowrun6Core.getItem(SR6Quality.class, earlyQual1);
	}

	//-------------------------------------------------------------------
	public void setEarlyAdultQuality1(SR6Quality value) {
		if (value==null) this.earlyQual1=null;
		else this.earlyQual1 = value.getId();
	}

	//-------------------------------------------------------------------
	public SR6Quality getEarlyAdultQuality2() {
		if (earlyQual2 == null) return null;
		return Shadowrun6Core.getItem(SR6Quality.class, earlyQual2);
	}

	//-------------------------------------------------------------------
	public void setEarlyAdultQuality2(SR6Quality value) {
		if (value==null) this.earlyQual2=null;
		else this.earlyQual2 = value.getId();
	}

	//-------------------------------------------------------------------
	public UUID getEarlyAdultContact() {
		return (earlyContact!=null)?UUID.fromString(earlyContact):null;
	}

	//-------------------------------------------------------------------
	public void setEarlyAdultContact(UUID value) {
		earlyContact = (value!=null)?value.toString():null;
	}

	//-------------------------------------------------------------------
	public ShadowrunAttribute getEarlyAdultAttribute() {
		return earlyAttribute;
	}

	//-------------------------------------------------------------------
	public void setEarlyAdultAttribute(ShadowrunAttribute attribute) {
		earlyAttribute = attribute;
	}

	//-------------------------------------------------------------------
	public PowerLevel getLifePathDefaultsAppliedFor() {
		return lifePathDefaultsAppliedFor;
	}

	//-------------------------------------------------------------------
	public void setLifePathDefaultsAppliedFor(PowerLevel value) {
		lifePathDefaultsAppliedFor = value;
	}
}
