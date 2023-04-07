package com.kiralin.editorimage

import javafx.beans.binding.Bindings
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.geometry.Point2D
import javafx.scene.control.TextField
import javafx.scene.layout.AnchorPane
import javafx.scene.paint.Paint
import javafx.scene.shape.Circle
import javafx.scene.shape.CubicCurve
import org.opencv.core.*
import java.io.IOException
import java.util.*

enum class ButtonTypes {
    INT, FLOAT, STRING, IMAGE, SEPIA, GREY, INVERT, BRIGHT, GAUSSIAN, SCALE_PIXEL,
    SCALE, MOVE_PIXEL, MOVE, ROTATE, ADD_TEXT_PIXEL, ADD_TEXT, FINISH_NODE
}

class NodeFunction {
    fun getNode(buttonTypes: ButtonTypes): DraggableNode {
        return when (buttonTypes) {
            ButtonTypes.INT -> IntNode()
            ButtonTypes.FLOAT -> FloatNode()
            ButtonTypes.STRING -> StringNode()
            ButtonTypes.IMAGE -> ImageNodeClass()
            ButtonTypes.SEPIA -> SepiaNode()
            ButtonTypes.GREY -> GreyNode()
            ButtonTypes.INVERT -> InvertNode()
            ButtonTypes.BRIGHT -> BrightnessNode()
            ButtonTypes.GAUSSIAN -> GaussianNode()
            ButtonTypes.SCALE_PIXEL -> ScalePixelNode()
            ButtonTypes.SCALE -> ScalePercentNode()
            ButtonTypes.MOVE_PIXEL -> MovePixelsNode()
            ButtonTypes.MOVE -> MovePercentNode()
            ButtonTypes.ROTATE-> RotateNode()
            ButtonTypes.ADD_TEXT_PIXEL -> AddTextPixelNode()
            ButtonTypes.ADD_TEXT -> AddTextPercentNode()
            ButtonTypes.FINISH_NODE -> EndNode()
        }
    }
}

data class NodeData(val id: String, val name: String, val x: Double?, val y: Double?,
                    var data: String?, val type: ButtonTypes?)

enum class NodeTypes {
    INT, FLOAT, STRING, IMAGE, NONE
}

class Colors {
    companion object {
        const val RED = "#FA8072"
        const val GREEN = "#008000"
        const val BLACK = "#ADFF2F"
    }
}

abstract class ValueNode : DraggableNode() {
    @FXML
    var value: TextField? = null

    override fun updateNode() {
        if (getValue() != null) {
            (outputLinkHandle!!.children.find { it is Circle } as Circle).fill = Paint.valueOf(Colors.GREEN)
        } else {
            (outputLinkHandle!!.children.find { it is Circle } as Circle).fill = Paint.valueOf(Colors.RED)
        }
    }

    override fun toData(): NodeData {
        val data = super.toData()
        data.data = value!!.text
        return data
    }

    override fun fromData(nodeData: NodeData) {
        super.fromData(nodeData)
        value!!.text = nodeData.data
    }

    init {
        init(UIFXML.VALUE_NODE)
    }
}

class IntNode : ValueNode() {
    override val nodeType: NodeTypes = NodeTypes.INT
    override fun addInit() {
        value!!.text = Titles.INT_DEF
        titleBar!!.text = Titles.INT

        value!!.textProperty().addListener { _, _, _ ->
            updateNode()
            outputLink?.kickAction()
        }
    }

    override fun getValue(): Int? {
        return value!!.text.toIntOrNull()
    }
}

class FloatNode : ValueNode() {

    override val nodeType: NodeTypes = NodeTypes.FLOAT

    override fun addInit() {
        value!!.text = Titles.FLOAT_DEF
        titleBar!!.text = Titles.FLOAT

        value!!.textProperty().addListener { _, _, _ ->
            updateNode()
            outputLink?.kickAction()
        }
    }

    override fun getValue(): Float? {
        return value!!.text.toFloatOrNull()
    }
}

class StringNode : ValueNode() {
    override val nodeType: NodeTypes = NodeTypes.STRING
    override fun addInit() {
        value!!.text = Titles.STRING_DEF
        titleBar!!.text = Titles.STRING

        value!!.textProperty().addListener { _, _, _ ->
            updateNode()
            outputLink?.kickAction()
        }
    }

    override fun getValue(): String {
        return value!!.text
    }
}

data class PairD<A, B>(val first: A, val second: B)

data class LinkData(
    val id: String?,
    val inputNode: String?,
    val inputNodeClass: String?,
    val outputNode: String?,
    val outputNodeClass: String?,
    val inputAnchor: String?,
    val inputAnchorSize: PairD<Double, Double>,
    val outputAnchor: String?,
    val outputAnchorSize: PairD<Double, Double>
)

class NodeLink : AnchorPane() {
    @FXML
    var nodeLink: CubicCurve? = null

    private var inputLinkString: String = ""
    private var inputNode: DraggableNode? = null
    private var outputNode: DraggableNode? = null
    private var inputAnchor: AnchorPane? = null
    private var outputAnchor: AnchorPane? = null

    @FXML
    private fun initialize() {
        nodeLink!!.controlX1Property().bind(
            Bindings.add(nodeLink!!.startXProperty(), 100))
        nodeLink!!.controlX2Property().bind(
            Bindings.add(nodeLink!!.endXProperty(), -100))
        nodeLink!!.controlY1Property().bind(
            Bindings.add(nodeLink!!.startYProperty(), 0))
        nodeLink!!.controlY2Property().bind(
            Bindings.add(nodeLink!!.endYProperty(), 0))

        parentProperty().addListener { _, _, _ ->
            if (parent == null) {
                if (inputNode != null) {
                    inputNode!!.connectedLinks.remove(this)
                    if (outputNode != null && inputNode!!.nodes.containsKey(inputLinkString)) {
                        inputNode!!.nodes[inputLinkString] =
                            inputNode!!.nodes[inputLinkString]!!.copy(second = null)
                    }
                }
                if (outputNode != null) {
                    outputNode!!.connectedLinks.remove(this)
                    outputNode!!.outputLink = null
                }
            }
        }
    }

    fun setStart(point: Point2D) {
        nodeLink!!.startX = point.x
        nodeLink!!.startY = point.y
    }

    fun setEnd(point: Point2D) {
        nodeLink!!.endX = point.x
        nodeLink!!.endY = point.y
    }

    fun bindStartEnd(source1: DraggableNode, source2: DraggableNode, a1: AnchorPane, a2: AnchorPane) {
        nodeLink!!.startXProperty().bind(
            Bindings.add(source1.layoutXProperty(), a1.layoutX + a1.width / 2.0))
        nodeLink!!.startYProperty().bind(
            Bindings.add(source1.layoutYProperty(), a1.layoutY + a1.height / 2.0))
        nodeLink!!.endXProperty().bind(
            Bindings.add(source2.layoutXProperty(), a2.layoutX + a2.width / 2.0))
        nodeLink!!.endYProperty().bind(
            Bindings.add(source2.layoutYProperty(), a2.layoutY + a2.height / 2.0))

        inputLinkString = a2.id
        outputAnchor = a1
        inputAnchor = a2
        links(source1, source2)
    }

    private fun links(source1: DraggableNode, source2: DraggableNode) {
        outputNode = source1
        inputNode = source2
        source1.connectedLinks.add(this)
        source2.connectedLinks.add(this)

        if (updateNode(outputNode!!))
            kickAction()
    }

    private fun updateNode(node: DraggableNode): Boolean {
        if (node.nodes.all { it.value.second != null }) {
            node.updateNode()
            return true
        }
        return false
    }

    fun kickAction() {
        if (inputNode == null)
            return

        if (updateNode(inputNode!!) && inputNode!!.outputLink != null)
            inputNode!!.outputLink!!.kickAction()
    }

    fun toData(): LinkData {
        return LinkData(
            id,
            inputNode?.id,
            inputNode!!::class.simpleName,
            outputNode?.id,
            outputNode!!::class.simpleName,
            inputAnchor?.id,
            PairD(inputAnchor!!.layoutX + inputAnchor!!.width / 2,
                inputAnchor!!.layoutY + inputAnchor!!.height / 2),
            outputAnchor?.id,
            PairD(
                outputAnchor!!.layoutX + outputAnchor!!.width / 2,
                outputAnchor!!.layoutY + outputAnchor!!.height / 2
            ),
        )
    }

    init {
        val fxmlLoader = FXMLLoader(
            javaClass.getResource(UIFXML.LINK_NODE)
        )
        fxmlLoader.setRoot(this)
        fxmlLoader.setController(this)
        try {
            fxmlLoader.load<Any>()
        } catch (exception: IOException) {
            throw RuntimeException(exception)
        }
        id = UUID.randomUUID().toString()
    }
}

