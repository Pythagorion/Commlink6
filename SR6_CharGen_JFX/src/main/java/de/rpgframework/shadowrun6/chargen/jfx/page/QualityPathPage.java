package de.rpgframework.shadowrun6.chargen.jfx.page;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.prelle.javafx.JavaFXConstants;
import org.prelle.javafx.Mode;
import org.prelle.javafx.OptionalNodePane;
import org.prelle.javafx.Page;
import org.prelle.javafx.layout.AutoBox;

import de.rpgframework.ResourceI18N;
import de.rpgframework.genericrpg.data.ComplexDataItem;
import de.rpgframework.genericrpg.data.Decision;
import de.rpgframework.jfx.GenericDescriptionVBox;
import de.rpgframework.jfx.RPGFrameworkJavaFX;
import de.rpgframework.shadowrun6.QualityPathStep;
import de.rpgframework.shadowrun6.QualityPathStepValue;
import de.rpgframework.shadowrun6.QualityPathValue;
import de.rpgframework.shadowrun6.Shadowrun6Tools;
import de.rpgframework.shadowrun6.chargen.charctrl.SR6CharacterController;
import de.rpgframework.shadowrun6.chargen.jfx.SR6CharacterViewLayout;
import de.rpgframework.shadowrun6.chargen.jfx.pane.VisualQualityPathPane;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;

/**
 * @author prelle
 *
 */
public class QualityPathPage extends Page {

	private final static Logger logger = System.getLogger(QualityPathPage.class.getPackageName());

	private final static ResourceBundle RES = ResourceBundle.getBundle(SR6CharacterViewLayout.class.getName());

	private SR6CharacterController control;
	private QualityPathValue selected;

	private AutoBox flex;
	private TextFlow pathDescr;
	private VisualQualityPathPane visual;
	private OptionalNodePane layout;

	private CheckBox[] checkBoxes;
	private HBox[] infoNodes;

	//-------------------------------------------------------------------
	public QualityPathPage(SR6CharacterController control, QualityPathValue data) {
		super(data.getResolved().getName());
		this.control = control;
		this.selected = data;
		initComponents();
		initLayout();
		initInteractivity();

		showDescription(selected.getResolved());
		refresh();
	}

	//-------------------------------------------------------------------
	private void initComponents() {
		pathDescr = new TextFlow();
		RPGFrameworkJavaFX.parseMarkupAndFillTextFlow(pathDescr, selected.getModifyable().getDescription());
		visual = new VisualQualityPathPane();
		visual.setData(selected.getResolved());

		checkBoxes = new CheckBox[selected.getResolved().getSteps().size()];
		infoNodes  = new HBox[checkBoxes.length];
		for (int i=0; i<checkBoxes.length; i++) {
			QualityPathStep step = selected.getResolved().getSteps().get(i);
			checkBoxes[i] = new CheckBox(step.getName());
			checkBoxes[i].setUserData(step);
			checkBoxes[i].getStyleClass().add(JavaFXConstants.STYLE_HEADING4);
			checkBoxes[i].selectedProperty().addListener( (ov,o,n) -> {
				if (n) {
					selectedStep(step);
				} else {
					deselectStep(step);
				}
			});
			infoNodes[i] = new HBox(20);
			logger.log(Level.WARNING, "Modifications of "+step.getId()+" = "+step.getOutgoingModifications());
			infoNodes[i].getChildren().add(getRequirementBox(step));
			infoNodes[i].getChildren().add(getModificationBox(step));
		}
	}

	//-------------------------------------------------------------------
	private void initLayout() {
		VBox boxes = new VBox(5);
		for (CheckBox tmp : checkBoxes) {
			boxes.getChildren().add(tmp);
			boxes.getChildren().add(infoNodes[ List.of(checkBoxes).indexOf(tmp)]);
		}

		VBox bxDescAndVisual = new VBox(20, pathDescr,visual);
		bxDescAndVisual.setStyle("-fx-min-width: 20em");
		bxDescAndVisual.setStyle("-fx-pref-width: 20em");
		//bxDescAndVisual.setStyle("-fx-max-width: 30em");
//		visual.prefHeight(400);
//		AcrylicPane acryl = new AcrylicPane(visual, false);
//		acryl.prefHeightProperty().bind(list.heightProperty());
		flex = new AutoBox();
		flex.setSpacing(20);
		flex.getContent().addAll(bxDescAndVisual, boxes);
//		flex.setBreakpoint(WindowMode.MINIMAL);

		layout = new OptionalNodePane(flex, new Label(ResourceI18N.get(RES,"select.for.description")));
		setContent(layout);
		super.setMode(Mode.REGULAR);
	}

	//-------------------------------------------------------------------
	private void initInteractivity() {
		visual.getSelectionModel().selectedItemProperty().addListener( (ov,o,n) -> {
			showDescription(n);
		});
	}

	//-------------------------------------------------------------------
	private void showDescription(ComplexDataItem n) {
		logger.log(Level.INFO, "Show description "+n);
		if (n==null) {
			layout.setOptional(null);
		} else {
			layout.setOptional( new GenericDescriptionVBox(
					Shadowrun6Tools.requirementResolver(Locale.getDefault()),
					Shadowrun6Tools.modificationResolver(Locale.getDefault()), n));
			layout.setTitle(n.getName());
		}
	}

	//-------------------------------------------------------------------
	public void refresh() {
		for (CheckBox box : checkBoxes) {
			QualityPathStep step = (QualityPathStep)box.getUserData();
			QualityPathStepValue taken = selected.getStepTaken(step);
			box.setSelected(taken!=null);
			if (box.isSelected()) {
				box.setDisable(!control.getQualityPathController().canBeDeselected(selected, taken).get());
			} else {
				box.setDisable(!control.getQualityPathController().canBeSelected(selected, step).get());
			}
		}
	}

	//-------------------------------------------------------------------
	private void selectedStep(QualityPathStep step) {

		control.getQualityPathController().select(selected, step);
		refresh();
	}

	//-------------------------------------------------------------------
	private void deselectStep(QualityPathStep step) {
		QualityPathStepValue stepVal = selected.getStepTaken(step);
		control.getQualityPathController().deselect(selected, stepVal);
	}

	//-------------------------------------------------------------------
	private VBox getRequirementBox(QualityPathStep item) {
		return RPGFrameworkJavaFX.getRequirementsBox(
				item,
				req -> Shadowrun6Tools.isRequirementMet(control.getModel(), item, req,new Decision[0]),
				Shadowrun6Tools.requirementResolver(Locale.getDefault())
				);
	}

	//-------------------------------------------------------------------
	private VBox getModificationBox(QualityPathStep item) {
		return RPGFrameworkJavaFX.getModificationsBox(
				item,
				m -> Shadowrun6Tools.getModificationString(item, m, Locale.getDefault())
				);
	}

}
