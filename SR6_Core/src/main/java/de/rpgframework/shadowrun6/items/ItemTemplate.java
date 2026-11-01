package de.rpgframework.shadowrun6.items;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.UUID;

import org.prelle.simplepersist.AttribConvert;
import org.prelle.simplepersist.Attribute;
import org.prelle.simplepersist.Element;
import org.prelle.simplepersist.ElementList;
import org.prelle.simplepersist.ElementListUnion;

import de.rpgframework.genericrpg.ValueType;
import de.rpgframework.genericrpg.data.ApplyTo;
import de.rpgframework.genericrpg.data.Choice;
import de.rpgframework.genericrpg.data.DataErrorException;
import de.rpgframework.genericrpg.data.DataItemTypeKey;
import de.rpgframework.genericrpg.data.DataSet;
import de.rpgframework.genericrpg.data.IReferenceResolver;
import de.rpgframework.genericrpg.data.ReferenceException;
import de.rpgframework.genericrpg.items.AGearData;
import de.rpgframework.genericrpg.items.CarryMode;
import de.rpgframework.genericrpg.items.IGearTypeData;
import de.rpgframework.genericrpg.items.IItemAttribute;
import de.rpgframework.genericrpg.items.ItemAttributeDefinition;
import de.rpgframework.genericrpg.items.OperationMode;
import de.rpgframework.genericrpg.items.PieceOfGear;
import de.rpgframework.genericrpg.items.PieceOfGearVariant;
import de.rpgframework.genericrpg.items.Usage;
import de.rpgframework.genericrpg.modification.DataItemModification;
import de.rpgframework.genericrpg.modification.EmbedModification;
import de.rpgframework.genericrpg.modification.Modification;
import de.rpgframework.genericrpg.modification.ValueModification;
import de.rpgframework.shadowrun.ShadowrunCharacter;
import de.rpgframework.shadowrun.items.Availability;
import de.rpgframework.shadowrun.persist.AvailabilityConverter;
import de.rpgframework.shadowrun6.Shadowrun6Core;
import de.rpgframework.shadowrun6.modifications.ShadowrunReference;

/**
 * @author prelle
 *
 */
@DataItemTypeKey(id = "item")
public class ItemTemplate extends PieceOfGear<SR6VariantMode,SR6UsageMode,SR6PieceOfGearVariant,SR6AlternateUsage> implements IReferenceResolver {

	private static String FLAG_NOWIFI = "NOWIFI";
	public final static UUID UUID_AUGMENTATION_QUALITY = UUID.fromString("c2d17c87-1cfe-4355-9877-a20fe09c170c");
	public final static UUID UUID_RATING = UUID.fromString("c2d17c87-1cfe-4355-9877-a20fe09c170d");
	public final static UUID UUID_AMMUNITION_TYPE = UUID.fromString("b015341d-24dc-42bb-a46b-781a5340e0b3");
	public final static UUID UUID_CHEMICAL_CHOICE = UUID.fromString("c50abe3b-2ba9-4b91-8509-e46605adf82c");
	public final static Choice CHOICE_AUGMENTATION_QUALITY = new Choice(
			ItemTemplate.UUID_AUGMENTATION_QUALITY,
			ShadowrunReference.AUGMENTATION_QUALITY);
	public final static UUID UUID_UNUSED_SOFTWARE_DEVICE = ShadowrunCharacter.UUID_UNUSED_SOFTWARE_DEVICE;
	public final static UUID UUID_UNARMED = UUID.fromString("9f05953f-0df5-4661-8114-3f02fcf2e669");
	public final static UUID UUID_VOLATILE_PRICEMOD = UUID.fromString("b61167ae-c12c-4bdb-a5da-a1a422405c35");

	public final static OperationMode MODE_WIRELESS_ON = new OperationMode("WIRELESS");

	@Attribute(name="avail",required=false)
	@AttribConvert(AvailabilityConverter.class)
	private Availability availability;
	@Attribute
	private ItemType type;
	@Attribute
	private ItemSubType subtype;

	@ElementListUnion({
		@ElementList(entry="weapon", type = WeaponData.class, inline = true),
		@ElementList(entry="armor", type=ArmorData.class, inline=true),
		@ElementList(entry="ammo", type=AmmunitionData.class, inline=true),
		@ElementList(entry="matrix", type=MatrixData.class, inline=true),
		@ElementList(entry="vehicle", type=VehicleData.class, inline=true),
		@ElementList(entry="alchemy", type=AlchemyData.class, inline=true),
	})
	private List<IGearTypeData> shortcuts;

	@Element(name="geardef")
	protected ItemTemplateList gearDef;

	//-------------------------------------------------------------------
	public ItemTemplate() {
		shortcuts = new ArrayList<>();
		super.refType = ShadowrunReference.ITEM_ATTRIBUTE;
	}

	//-------------------------------------------------------------------
	public ItemAttributeDefinition getAttribute(IItemAttribute attrib, SR6PieceOfGearVariant variant) {
		ItemAttributeDefinition def = null;
		if (variant!=null) {
			def = variant.getAttribute(attrib);
			if (def!=null)
				return def;
		}

		return cache.get(attrib);
	}

//	//-------------------------------------------------------------------
//	public Choice getChoice(UUID uuid) {
//		if (UUID_AUGMENTATION_QUALITY.equals(uuid))
//			return CHOICE_AUGMENTATION_QUALITY;
//		return super.getChoice(uuid);
//	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.genericrpg.data.AGearData#getTypeData()
	 */
	@Override
	public List<? extends IGearTypeData> getTypeData() {
//		ArrayList<IGearTypeData> ret = new ArrayList<>();
//		if (!weapons.isEmpty())
//			ret.add(weapons.get(0));
		return shortcuts;
	}

	//-------------------------------------------------------------------
	public List<WeaponData> getAttacks() {
		ArrayList<WeaponData> ret = new ArrayList<>();
		for (IGearTypeData tmp : shortcuts) {
			if (tmp instanceof WeaponData) {
				ret.add((WeaponData) tmp);
			}
		}
		// Add from usages
		for (SR6AlternateUsage usage : super.getAlternates()) {
			logger.log(Level.WARNING, "ToDo: Usage: "+usage);
			ret.addAll(usage.getAttacks());
		}

		return ret;
	}

	//-------------------------------------------------------------------
	public ItemTemplate resolveItem(String key) {
		if (gearDef==null) return null;

		for (ItemTemplate tmp : gearDef) {
			if (tmp.getId().equals(key)) {
				tmp.validate();
				return tmp;
			}
		}
		return null;
	}

	//-------------------------------------------------------------------
	public List<ItemTemplate> getGearSubDefinitions() {
		if (gearDef==null) return List.of();

		return gearDef;
	}

	//-------------------------------------------------------------------
	@Override
	public void validate() {
//		attributes.clear();
		if (type==null) throw new DataErrorException(this, "sort type not set for '"+id+"'");
		if (subtype==null) throw new DataErrorException(this, "sort subtype not set for '"+id+"'");

		setAttribute(SR6ItemAttribute.PRICE, super.price);
		setAttribute(SR6ItemAttribute.ITEMTYPE, type);
		setAttribute(SR6ItemAttribute.ITEMSUBTYPE, subtype);
		super.validate();

		if (availability!=null)
			setAttribute(SR6ItemAttribute.AVAILABILITY, availability);

		/* If there is no USAGE assume a CARRIED mode and no slot */
		if (usages.isEmpty()) {
			if (requireVariant) {
				// Ensure all variants have a USAGE mode
				for (SR6PieceOfGearVariant variant : variants) {
					if (variant.getUsages()==null || variant.getUsages().isEmpty()) {
						variant.addUsage(new Usage(CarryMode.CARRIED));
					}
				}
			} else {
				usages.add(new Usage(CarryMode.CARRIED));
			}
		}

		// Validate flags
		for (String flag : getFlags()) {
			try {
				SR6ItemFlag.valueOf(flag);
			} catch (IllegalArgumentException e) {
				throw new DataErrorException(this, "No such flag '"+flag+"'");
			}
		}

		// Validate gear definitions
		if (gearDef != null) {
			for (ItemTemplate tmp : gearDef) {
				tmp.setParentItem(this);
				tmp.assignToDataSet(this.datasets.get(0));
			}
		}

		// TODO
		// Make a implant version of every weapon
		// for that remove normal accessory slots and replace them with
		// implant versions


		// Validate hook identifier in modifications
		for (Modification tmp : getOutgoingModifications()) {
			tmp.setSource(this);
			tmp.validate();
			if (tmp instanceof EmbedModification) {
				try {
					((EmbedModification)tmp).getHook();
					//((EmbedModification)tmp).getResolvedKey();
				} catch (ClassCastException e) {
					throw new DataErrorException(this, "Internal class cast error: "+e.getMessage());
				} catch (ReferenceException e) {
					throw new DataErrorException(this, e.getError());
				} catch (Exception e) {
					e.printStackTrace();
					throw new DataErrorException(this, "Internal error: "+e.getMessage());
				}

			}
		}

		validateModificationSlots();
		validateAutosoftsForVehicles();

		// If it is a firearm, mark it that the user may select to use caseless
		// ammo with it
		if (this.getItemType(CarryMode.CARRIED)==ItemType.WEAPON_FIREARMS || this.getItemType(CarryMode.CARRIED)==ItemType.WEAPON_SPECIAL) {
			validateWeaponSize();
		}

		// If it is an armor, add fashion slots (Smooth Operations)
		if (this.getItemType(CarryMode.CARRIED)==ItemType.ARMOR) {
			validateFashionSlots();
		}

		validateVehicleWeaponMountSlots();
		if (this.getItemType(CarryMode.CARRIED)==ItemType.WEAPON_FIREARMS || this.getItemType(CarryMode.CARRIED)==ItemType.WEAPON_SPECIAL) {
			// Auto-detect ammunition class, if necessary
			validateAmmunitionClass();

			if (!flags.contains(SR6ItemFlag.NO_CASELESS_AMMO.name())) {
				addUserSelectableFlag(SR6ItemFlag.USES_CASELESS);
			}
			// Also add item hook for implanted weapons
			if (getUsage(CarryMode.EMBEDDED)==null || (getUsage(CarryMode.EMBEDDED).getSlot()!=null) && !getUsage(CarryMode.EMBEDDED).getSlot().name().startsWith("IMPLANT")) {
				Usage implantUse = new Usage(CarryMode.EMBEDDED);
				switch (subtype) {
				case BLADES:
				case WHIPS:
				case CLUBS:
					implantUse.setSlot(ItemHook.IMPLANT_MELEE); break;
				case BOWS:
				case CROSSBOWS:
					implantUse.setSlot(ItemHook.IMPLANT_RANGED); break;
				case TASERS:
				case HOLDOUTS:
					implantUse.setSlot(ItemHook.IMPLANT_HOLDOUT); break;
				case PISTOLS_LIGHT  : implantUse.setSlot(ItemHook.IMPLANT_PISTOL_LIGHT); break;
				case MACHINE_PISTOLS: implantUse.setSlot(ItemHook.IMPLANT_PISTOL_MACHINE); break;
				case PISTOLS_HEAVY  : implantUse.setSlot(ItemHook.IMPLANT_PISTOL_HEAVY); break;
				case SUBMACHINE_GUNS: implantUse.setSlot(ItemHook.IMPLANT_SMG); break;
				case SHOTGUNS: implantUse.setSlot(ItemHook.IMPLANT_SHOTGUN); break;
				case LAUNCHERS:
				case RIFLE_ASSAULT:
				case RIFLE_HUNTING:
				case RIFLE_SNIPER:
				case LMG:
				case MMG:
				case HMG:
				case ASSAULT_CANNON:
				case THROWERS:
					implantUse.setSlot(ItemHook.IMPLANT_HEAVY); break;
					// not assessing BALLISTAS, CANNON and VEHICLE_MILITARY, as those cannot be implanted
				case OTHER_SPECIAL:
					WeaponSize size = (this.getAttribute(SR6ItemAttribute.WEAPON_SIZE)!=null)?this.getAttribute(SR6ItemAttribute.WEAPON_SIZE).getValue():null;
					if (size!=null) {
						switch (size) {
						case TINY: // TASERS, HOLDOUTS, PISTOLS_LIGHT sized
							implantUse.setSlot(ItemHook.IMPLANT_PISTOL_LIGHT); break;
						case SMALL: // PISTOLS_HEAVY, MACHINE_PISTOLS, DMSO, DART sized
							implantUse.setSlot(ItemHook.IMPLANT_PISTOL_HEAVY); break;
						case MEDIUM: // SUBMACHINE_GUNS sized
							implantUse.setSlot(ItemHook.IMPLANT_SMG); break;
						case LARGE: // SHOTGUNS sized
							implantUse.setSlot(ItemHook.IMPLANT_SHOTGUN); break;
						// not assessing sizes BIG or MILITARY, as those cannot be implanted
						}
					}
					break;
				}
			this.addUsage(implantUse);
			}
		}

		// If this is a drone or vehicle, ensure there is a SOFTWARE slot with Pilot/2 round up capacity
		if (ItemType.isDrone(type) || ItemType.isVehicle(type) || type.name().contains("DRONE") || type.name().contains("VEHICLES")) {
			this.countable = true;
			boolean hasSoftwareSlot = getOutgoingModifications().stream().anyMatch( mod -> (mod instanceof DataItemModification) && mod.getReferenceType()==ShadowrunReference.HOOK && ((DataItemModification)mod).getKey().equals(ItemHook.SOFTWARE.name()));
			if (!hasSoftwareSlot) {
				// Auto-add modification
				double pilot = 0;
				if (this.getAttribute(SR6ItemAttribute.PILOT)!=null) {
					pilot = getAttribute(SR6ItemAttribute.PILOT).getDistributed();
				} else if (shortcuts.stream().allMatch(sc -> sc instanceof VehicleData)) {
					VehicleData vdata = (VehicleData) shortcuts.stream().filter(sc -> sc instanceof VehicleData).findFirst().get();
					pilot = vdata.getPilot();
				}
				if (pilot>0) {
					ValueModification autoMod = new ValueModification(ShadowrunReference.HOOK, ItemHook.SOFTWARE.name(), (int)Math.round( pilot/2.0d));
					autoMod.setSet(ValueType.NATURAL);
					autoMod.setApplyTo(ApplyTo.DATA_ITEM);
					modifications.add(autoMod);
//					if (id.contains("cyberspace_designs_quadrotor")) {
//						System.err.println("Auto-mod "+autoMod);
//						System.exit(1);
//					}
				}
			}
		}


		// If it has an AUGMENTATION flag, add that decision
		if (flags.contains(SR6ItemFlag.AUGMENTATION.name()) && this.getChoice(ItemTemplate.UUID_AUGMENTATION_QUALITY)==null) {
			addChoice(CHOICE_AUGMENTATION_QUALITY);
		}
		if (variants!=null) {
			for (SR6PieceOfGearVariant variant : variants) {
				variant.setParentItem(this);
				variant.validate();
				// Validate flags
				for (String flag : variant.getFlags()) {
					try {
						SR6ItemFlag.valueOf(flag);
					} catch (IllegalArgumentException e) {
						throw new DataErrorException(this, "No such flag '"+flag+"' (in variant)");
					}
				}

				if (variant.hasFlag(SR6ItemFlag.AUGMENTATION.name()) && variant.getChoice(UUID_AUGMENTATION_QUALITY)==null) {
					variant.addChoice(CHOICE_AUGMENTATION_QUALITY);
				}
			}
		}

	}

	//-------------------------------------------------------------------
	private void validateAmmunitionClass() {
		if (getAttribute(SR6ItemAttribute.AMMUNITION_CLASS)==null) {
		switch (subtype) {
		case TASERS:  this.setAttribute(SR6ItemAttribute.AMMUNITION_CLASS, AmmunitionClass.TASER); break;
		case HOLDOUTS:
		case PISTOLS_LIGHT:
		case MACHINE_PISTOLS:
			this.setAttribute(SR6ItemAttribute.AMMUNITION_CLASS, AmmunitionClass.LIGHT);
			break;
		case PISTOLS_HEAVY:
		case SUBMACHINE_GUNS:
			this.setAttribute(SR6ItemAttribute.AMMUNITION_CLASS, AmmunitionClass.HEAVY);
			break;
		case SHOTGUNS: this.setAttribute(SR6ItemAttribute.AMMUNITION_CLASS, AmmunitionClass.SHOTGUN); break;
		case RIFLE_ASSAULT:
		case RIFLE_HUNTING:
		case RIFLE_SNIPER:
			this.setAttribute(SR6ItemAttribute.AMMUNITION_CLASS, AmmunitionClass.RIFLES); break;
		case LMG:
		case MMG:
		case HMG:
			this.setAttribute(SR6ItemAttribute.AMMUNITION_CLASS, AmmunitionClass.MACHINE_GUN);
			break;
		case ASSAULT_CANNON:
			this.setAttribute(SR6ItemAttribute.AMMUNITION_CLASS, AmmunitionClass.ASSAULT_CANNON); break;
		case DMSO:
			this.setAttribute(SR6ItemAttribute.AMMUNITION_CLASS, AmmunitionClass.DMSO); break;
		case DART:
			this.setAttribute(SR6ItemAttribute.AMMUNITION_CLASS, AmmunitionClass.INJECTION_DART); break;
		default:
			logger.log(Level.WARNING, "No autodetection of ammunition for firearm {0}",subtype);
		}
		} // End of ammunition check, not assessing OTHER_SPECIAL and VEHICLE_MILITARY as those have unique ammo per weapon
	}

	//-------------------------------------------------------------------
	private void validateWeaponSize() {
		if (getAttribute(SR6ItemAttribute.WEAPON_SIZE)==null) {
		switch (subtype) {
		case TASERS:
		case HOLDOUTS:
		case PISTOLS_LIGHT:
			this.setAttribute(SR6ItemAttribute.WEAPON_SIZE, WeaponSize.TINY);
			break;
		case MACHINE_PISTOLS:
		case PISTOLS_HEAVY:
		case DMSO:
		case DART:
			this.setAttribute(SR6ItemAttribute.WEAPON_SIZE, WeaponSize.SMALL);
			break;
		case SUBMACHINE_GUNS:
			this.setAttribute(SR6ItemAttribute.WEAPON_SIZE, WeaponSize.MEDIUM);
			break;
		case SHOTGUNS:
		case RIFLE_ASSAULT:
			this.setAttribute(SR6ItemAttribute.WEAPON_SIZE, WeaponSize.LARGE); break;
		case RIFLE_HUNTING:
		case RIFLE_SNIPER:
		case LMG:
		case MMG:
		case HMG:
		case ASSAULT_CANNON:
		case LAUNCHERS:
		case THROWERS:
		case BALLISTAS:
		case CANNON:
			this.setAttribute(SR6ItemAttribute.WEAPON_SIZE, WeaponSize.BIG);
			break;
		case VEHICLE_MILITARY:
			this.setAttribute(SR6ItemAttribute.WEAPON_SIZE, WeaponSize.MILITARY);
			break;
		default:
			logger.log(Level.WARNING, "No autodetection of weapon size for firearm {0}",subtype);
		}
		} // End of ammunition check
	}

	//-------------------------------------------------------------------
	private void validateVehicleWeaponMountSlots() {
		ItemType carriedType = this.getItemType(CarryMode.CARRIED);
		if (carriedType==null || !ItemType.isWeapon(carriedType))
			return;
		ItemSubType carriedSubtype = this.getItemSubtype(CarryMode.CARRIED);

		// define firearms based on weapon size		
		if (carriedType==ItemType.WEAPON_FIREARMS || carriedType==ItemType.WEAPON_SPECIAL || carriedType==ItemType.WEAPON_VEHICLE) {
		WeaponSize size = (this.getAttribute(SR6ItemAttribute.WEAPON_SIZE)!=null)?this.getAttribute(SR6ItemAttribute.WEAPON_SIZE).getValue():null;
			if (size!=null) {
			switch (size) {
				case BUILDING: // Kechibi hall of tears 
					break;

				case MILITARY: // military weapons from Deadly Arts having Size 1+
					this.addUsage(new Usage(CarryMode.EMBEDDED, ItemHook.VEHICLE_WEAPON_MILITARY)); // TODO: need 1 military hardpoint per weapon size, but not representing size yet
					break;
			
				case BIG: // all other
					this.addUsage(new Usage(CarryMode.EMBEDDED, ItemHook.VEHICLE_WEAPON_LARGE)); // Double Clutch 142 "any man-portable weapon"
					break;

				case LARGE: // RIFLE_ASSAULT, SHOTGUNS
					this.addUsage(new Usage(CarryMode.EMBEDDED, ItemHook.VEHICLE_WEAPON)); // Double Clutch 142 "any assault rifle or smaller firearm"
					this.addUsage(new Usage(CarryMode.EMBEDDED, ItemHook.VEHICLE_WEAPON_LARGE));
					break;

				case TINY: // TASERS, etc.
				case SMALL: // PISTOLS_HEAVY, etc.
				case MEDIUM: // SMG
					this.addUsage(new Usage(CarryMode.EMBEDDED, ItemHook.VEHICLE_WEAPON_SMALL)); // Double Clutch 142 "any SMG or smaller firearm"
					this.addUsage(new Usage(CarryMode.EMBEDDED, ItemHook.VEHICLE_WEAPON));
					this.addUsage(new Usage(CarryMode.EMBEDDED, ItemHook.VEHICLE_WEAPON_LARGE));
					break;

				}
			return;
			}
		}
		
		// if no weapon size found, use subtype
		switch (carriedSubtype) {
		case BLADES:
		case WHIPS:
		case CLUBS:
		case BOWS:
		case CROSSBOWS:
		case RIFLE_HUNTING:
		case RIFLE_SNIPER:
		case LMG:
		case MMG:
		case HMG:
		case LAUNCHERS:
		case THROWERS:
			this.addUsage(new Usage(CarryMode.EMBEDDED, ItemHook.VEHICLE_WEAPON_LARGE)); // Double Clutch 142 "any man-portable weapon"
			break;

		case SHOTGUNS:
		case RIFLE_ASSAULT:
			this.addUsage(new Usage(CarryMode.EMBEDDED, ItemHook.VEHICLE_WEAPON)); // Double Clutch 142 "any assault rifle or smaller firearm"
			this.addUsage(new Usage(CarryMode.EMBEDDED, ItemHook.VEHICLE_WEAPON_LARGE));
			break;

		case TASERS:
		case HOLDOUTS:
		case PISTOLS_LIGHT  :
		case MACHINE_PISTOLS:
		case PISTOLS_HEAVY  :
		case SUBMACHINE_GUNS:
			this.addUsage(new Usage(CarryMode.EMBEDDED, ItemHook.VEHICLE_WEAPON_SMALL)); // Double Clutch 142 "any SMG or smaller firearm"
			this.addUsage(new Usage(CarryMode.EMBEDDED, ItemHook.VEHICLE_WEAPON));
			this.addUsage(new Usage(CarryMode.EMBEDDED, ItemHook.VEHICLE_WEAPON_LARGE));
			break;
		}
	}

	//-------------------------------------------------------------------
	private void validateModificationSlots() {
		if (hasFlag(SR6ItemFlag.UNMODIFYABLE.name())) {
			setAttribute(SR6ItemAttribute.MODIFICATION_SLOTS, 0);
			return;
		}
		
		switch (type) {
		case WEAPON_CLOSE_COMBAT:
			setAttribute(SR6ItemAttribute.MODIFICATION_SLOTS, 2);
			break;
		case WEAPON_RANGED:
			if (subtype==ItemSubType.THROWING) {
				setAttribute(SR6ItemAttribute.MODIFICATION_SLOTS, 0);
			} else {
				setAttribute(SR6ItemAttribute.MODIFICATION_SLOTS, 2);
			}
			break;
		case WEAPON_FIREARMS:
			switch (subtype) {
			case TASERS       : setAttribute(SR6ItemAttribute.MODIFICATION_SLOTS, 2); break;
			case HOLDOUTS     : setAttribute(SR6ItemAttribute.MODIFICATION_SLOTS, 0); break;
			case PISTOLS_LIGHT: setAttribute(SR6ItemAttribute.MODIFICATION_SLOTS, 3); break;
			case PISTOLS_HEAVY: setAttribute(SR6ItemAttribute.MODIFICATION_SLOTS, 3); break;
			case MACHINE_PISTOLS: setAttribute(SR6ItemAttribute.MODIFICATION_SLOTS, 3); break;
			case SUBMACHINE_GUNS: setAttribute(SR6ItemAttribute.MODIFICATION_SLOTS, 4); break;
			case SHOTGUNS      : setAttribute(SR6ItemAttribute.MODIFICATION_SLOTS, 5); break;
			case RIFLE_SNIPER  : setAttribute(SR6ItemAttribute.MODIFICATION_SLOTS, 5); break;
			case RIFLE_HUNTING : setAttribute(SR6ItemAttribute.MODIFICATION_SLOTS, 5); break;
			case RIFLE_ASSAULT : setAttribute(SR6ItemAttribute.MODIFICATION_SLOTS, 6); break;
			case LMG           : setAttribute(SR6ItemAttribute.MODIFICATION_SLOTS, 4); break;
			case MMG           : setAttribute(SR6ItemAttribute.MODIFICATION_SLOTS, 4); break;
			case HMG           : setAttribute(SR6ItemAttribute.MODIFICATION_SLOTS, 4); break;
			case ASSAULT_CANNON: setAttribute(SR6ItemAttribute.MODIFICATION_SLOTS, 4); break;
			case VEHICLE_MILITARY: setAttribute(SR6ItemAttribute.MODIFICATION_SLOTS, 0); break;
			case BALLISTAS     : setAttribute(SR6ItemAttribute.MODIFICATION_SLOTS, 0); break;
			case CANNON        : setAttribute(SR6ItemAttribute.MODIFICATION_SLOTS, 0); break;
			}
			break;
		case WEAPON_SPECIAL:
			setAttribute(SR6ItemAttribute.MODIFICATION_SLOTS, 2);
			break;
		case CYBERWARE: // CYBER_IMPLANT_WEAPON to allow installation of 'ergonomic_optimization' from Deadly Arts 31, CYBER_LIMBS to allow installation of 'bulk modification' from Body Shop 44
			if (subtype==ItemSubType.CYBER_IMPLANT_WEAPON||subtype==ItemSubType.CYBER_LIMBS) {
				setAttribute(SR6ItemAttribute.MODIFICATION_SLOTS, 1);
			} else {
				setAttribute(SR6ItemAttribute.MODIFICATION_SLOTS, 0);
			}
			break;
		case AMMUNITION:
			switch (subtype) {
			case GRENADES:
			case AMMUNITION:
				setAttribute(SR6ItemAttribute.MODIFICATION_SLOTS, 1);
				break;
			}
			break;
		case VEHICLES:
		case DRONE_MICRO:
		case DRONE_MINI:
		case DRONE_SMALL:
		case DRONE_MEDIUM:
		case DRONE_LARGE:
			int body = getAttribute(SR6ItemAttribute.BODY).getDistributed();
			int pt_slots = body;
			if (subtype==ItemSubType.MOD_TRAILER) {
				pt_slots = 0;
			}
//			// Determine Cargo Factor
//			int cf = 0;
//			if (getAttribute(SR6ItemAttribute.CARGO)!=null)
//				getAttribute(SR6ItemAttribute.CARGO).getDistributed();
//			if (cf == 0) {
//				if (getAttribute(SR6ItemAttribute.SEATS)!=null)
//					cf = getAttribute(SR6ItemAttribute.SEATS).getDistributed();
//				else
//					logger.log(Level.WARNING, "No information for seats/Cargo Factor for {0}", id);
//				// CF is "seats" *1,25 for subtype BIKES, "seats" *1,5
//				// for all others, +4 for subtype TRUCKS
//				if (subtype==ItemSubType.BIKES) {
//					if (Locale.getDefault().getLanguage().equals("de")) {
//						cf = (int) Math.round(cf * 0.25);
//					} else {
//						cf = (int) Math.round(cf * 1.25);
//					}
//				} else {
//					if (Locale.getDefault().getLanguage().equals("de")) {
//						cf = (int) Math.round(cf * 0.5);
//					} else {
//						cf = (int) Math.round(cf * 1.5);
//					}
//					if (subtype==ItemSubType.TRUCKS) {
//						cf += 4;
//					}
//				}
//			}

			int[] modSlots = new int[] {body,body,pt_slots};
			setAttribute(SR6ItemAttribute.VEHICLE_MODSLOTS, modSlots);
//			modifications.add(new ValueModification(ShadowrunReference.ITEM_ATTRIBUTE, SR6ItemAttribute.VEHICLE_MODSLOTS.name(), String.format("%d,%d,%d", body,body,pt_slots)));
			modifications.add(new ValueModification(ShadowrunReference.HOOK, ItemHook.VEHICLE_ACCESSORY.name(), 99));
//			if (cf>0)
//				modifications.add(new ValueModification(ShadowrunReference.HOOK, ItemHook.VEHICLE_CF.name(), cf));
			break;
		}
	}

	//-------------------------------------------------------------------
	private void validateAutosoftsForVehicles() {
		if (ItemType.isVehicle(type) && getAttribute(SR6ItemAttribute.SOFTWARE_TYPES)==null) {
			setAttribute(SR6ItemAttribute.SOFTWARE_TYPES, SoftwareTypes.AUTOSOFT);
		}
	}

	//-------------------------------------------------------------------
	private void validateFashionSlots() {
		if (subtype!=ItemSubType.ARMOR_BODY && subtype!=ItemSubType.ARMOR_CLOTHES && subtype!=ItemSubType.ARMOR_SOCIAL) 
			return;
		
		if (getAttribute(SR6ItemAttribute.DEFENSE_SOCIAL)==null)
			return;
		int social = getAttribute(SR6ItemAttribute.DEFENSE_SOCIAL).getDistributed();
		if (social<0) {
			modifications.add(new ValueModification(ShadowrunReference.HOOK, ItemHook.FASHION.name(), 1));
		} else if (social>0) {
			modifications.add(new ValueModification(ShadowrunReference.HOOK, ItemHook.FASHION.name(), 3));
		} else
			modifications.add(new ValueModification(ShadowrunReference.HOOK, ItemHook.FASHION.name(), 2));
	}

//	//-------------------------------------------------------------------
//	public List<SR6GearUsage> getAlternates() {
//		if (alternates.isEmpty()) {
//			return Arrays.asList(new SR6GearUsage(SR6UsageMode.PRIMARY));
//		}
//		return alternates;
//	}

	//-------------------------------------------------------------------
	/**
	 * @return the type
	 * @deprecated Use getItemType(CarryMode)
	 */
	@Deprecated
	public ItemType getItemType() {
		return type;
	}

	//-------------------------------------------------------------------
	public ItemType getItemType(CarryMode carry) {
		if (getUsage(carry)!=null) return type;
		SR6PieceOfGearVariant variant = (SR6PieceOfGearVariant) getVariant(carry);
		if (variant!=null && variant.getUsage(carry)!=null) {
			if (variant.getAttribute(SR6ItemAttribute.ITEMTYPE)!=null)
				return variant.getAttribute(SR6ItemAttribute.ITEMTYPE).getValue();
			return type;
		}
		if (usages==null || usages.isEmpty()) return type;
		return null;
	}

	//-------------------------------------------------------------------
	public AGearData getMainOrVariant(CarryMode carry) {
		if (getUsage(carry)!=null) return this;
		SR6PieceOfGearVariant variant = (SR6PieceOfGearVariant) getVariant(carry);
		if (variant!=null && variant.getUsage(carry)!=null) {
			return variant;
		}
		return this;
	}

	//-------------------------------------------------------------------
	/**
	 * @param type the type to set
	 */
	public void setItemType(ItemType type) {
		this.type = type;
	}

	//-------------------------------------------------------------------
	/**
	 * @return the subtype
	 * @deprecated Use getItemSubtype(CarryMode)
	 */
	@Deprecated
	public ItemSubType getItemSubtype() {
		return subtype;
	}

//	//-------------------------------------------------------------------
//	public boolean isType(List<ItemType> values) {
//		return values
//		if (values.contains(this.type))
//			return true;
//		for (UseAs use : usedAs) {
//			if (values.contains(use.getType()))
//				return true;
//		}
//		return false;
//	}

	//-------------------------------------------------------------------
	public ItemSubType getItemSubtype(CarryMode carry) {
		if (getUsage(carry)!=null) return subtype;
		SR6PieceOfGearVariant variant = (SR6PieceOfGearVariant) getVariant(carry);
		if (variant!=null && variant.getUsage(carry)!=null) {
			if (variant.getAttribute(SR6ItemAttribute.ITEMSUBTYPE)!=null)
				return variant.getAttribute(SR6ItemAttribute.ITEMSUBTYPE).getValue();
			return subtype;
		}
		if (usages==null || usages.isEmpty()) return subtype;
		return null;
	}

	//-------------------------------------------------------------------
	/**
	 * @param subtype the subtype to set
	 */
	public void setItemSubtype(ItemSubType subtype) {
		this.subtype = subtype;
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.genericrpg.data.DataItem#assignToDataSet(de.rpgframework.genericrpg.data.DataSet)
	 */
	@Override
	public void assignToDataSet(DataSet set) {
		super.assignToDataSet(set);
		for (SR6PieceOfGearVariant var : variants) {
			var.setParentItem(this);
			var.assignToDataSet(set);
		}
	}

	//-------------------------------------------------------------------
	public Collection<String> getWiFiAdvantageStrings(CarryMode carry, PieceOfGearVariant variant, Locale locale) {
		List<String> ret = new ArrayList<>();

		String key = "item."+id+".wifi";
		String multiLine = getLocalizedString(locale, key);
		if (!multiLine.equals(key)) {
			StringTokenizer tok = new StringTokenizer(multiLine,"\n");
			while (tok.hasMoreTokens())
				ret.add(tok.nextToken());
		}
		// For firearms, add general wireless functionality
		ItemType type = getItemType(carry);
		List<ItemType> firearms = Arrays.asList(ItemType.WEAPON_FIREARMS, ItemType.WEAPON_SPECIAL);
		if (firearms.contains(type) && !hasFlag(ItemTemplate.FLAG_NOWIFI)) {
			StringTokenizer tok = new StringTokenizer(Shadowrun6Core.getI18nResources().getString("wireless.firearms_general", locale),"\n");
			while (tok.hasMoreTokens())
				ret.add(tok.nextToken());
		}

		// TODO: Variant

		return ret;
	}

	//-------------------------------------------------------------------
	public String getOverdrive(Locale locale) {
		String key = "item."+id+".overdrive";
		String line = getLocalizedString(locale, key);
		if (!line.equals(key)) {
			return line;
		}
		return null;
	}

	//-------------------------------------------------------------------
	public String getWiFi(Locale locale) {
		String key = "item."+id+".wifi";
		String line = getLocalizedString(locale, key);
		if (!line.equals(key)) {
			return line;
		}
		return null;
	}

	//-------------------------------------------------------------------
	public List<AGearData> getPossibilities(CarryMode carry) {
		List<AGearData> ret = new ArrayList<>();
		if (getUsage(carry)!=null && !requiresVariant())
			ret.add(ItemUtil.calculateVirtualItem(this, null, carry));
		for (SR6PieceOfGearVariant variant : getVariants()) {
			if (variant.getUsage(carry)!=null || (getUsage(carry)!=null && requiresVariant())) {
				ret.add(ItemUtil.calculateVirtualItem(this, variant, carry));
			}
		}
		return ret;
	}

	//-------------------------------------------------------------------
	public boolean isAugmentation() {
		ItemAttributeDefinition attr = getAttribute(SR6ItemAttribute.ITEMTYPE);
		return hasFlag(SR6ItemFlag.AUGMENTATION.name()) || attr.getValue()==ItemType.CYBERWARE || attr.getValue()==ItemType.BIOWARE;
	}

	//-------------------------------------------------------------------
	public boolean isMatrixDevice() {
		ItemAttributeDefinition attr = getAttribute(SR6ItemAttribute.ITEMTYPE);
		ItemSubType sub = getAttribute(SR6ItemAttribute.ITEMSUBTYPE).getValue();
		return hasFlag(SR6ItemFlag.MATRIX_DEVICE.name()) || attr.getValue()==ItemType.ELECTRONICS &&
				( sub==ItemSubType.COMMLINK ||  sub==ItemSubType.CYBERDECK ||  sub==ItemSubType.DATATERM ||  sub==ItemSubType.CYBERTERM ||  sub==ItemSubType.COMMNODE ||  sub==ItemSubType.RIGGER_CONSOLE  ||  sub==ItemSubType.TAC_NET );
	}

	//-------------------------------------------------------------------
	protected Choice getHardcodedChoice(UUID uuid) {
		if (CHOICE_AUGMENTATION_QUALITY.getUUID().equals(uuid))
			return CHOICE_AUGMENTATION_QUALITY;

		return null;
	}

}

class VariantItemTemplate extends ItemTemplate {

	SR6PieceOfGearVariant variant;

	public VariantItemTemplate(SR6PieceOfGearVariant variant) {
		this.variant = variant;
	}

	//--------------------------------------------------------------------
	public String getName(Locale locale) {
		if (parentItem!=null) {
			String key = parentItem.getTypeString()+"."+parentItem.getId().toLowerCase()+".variant."+id.toLowerCase();
			return getLocalizedString(locale, key);
		}
		String key = getTypeString()+"."+id.toLowerCase();
		return getLocalizedString(locale, key);
	}
}
