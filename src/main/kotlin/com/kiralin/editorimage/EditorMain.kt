package com.kiralin.editorimage

import com.google.gson.Gson
import javafx.application.Application
import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import javafx.stage.Stage
import java.io.File
import java.io.IOException

data class Saved(val nodes: MutableList<NodeData>?,
                 val links: MutableList<LinkData>?)

class mainDnD: Application() {
    private val width = 1280.0
    private val height = 720.0

    private var root = AnchorPane()
    private var scene = Scene(root, width, height)

    private val btnTypes: Array<ButtonTypes> = arrayOf(
        ButtonTypes.INT, ButtonTypes.FLOAT, ButtonTypes.STRING, ButtonTypes.IMAGE, ButtonTypes.SEPIA,
        ButtonTypes.GREY, ButtonTypes.INVERT, ButtonTypes.BRIGHT, ButtonTypes.GAUSSIAN,
        ButtonTypes.ROTATE, ButtonTypes.SCALE_PIXEL, ButtonTypes.SCALE, ButtonTypes.MOVE_PIXEL,
        ButtonTypes.MOVE, ButtonTypes.ADD_TEXT_PIXEL, ButtonTypes.ADD_TEXT
    )

    private fun BtnAdd(): VBox {
        val vbox = VBox(20.0)
        vbox.style = "-fx-padding: 20px 10px; -fx-background-color: #5F9EA0;" +
                "-fx-background-radius: 15px; -fx-border-radius: 15px;"

        fun BtnCreate(buttonTypes: ButtonTypes) {
            val button = Button(buttonTypes.toString())
            button.style = "-fx-padding: 5px 10px; -fx-margin: 10px;" +
                    " -fx-text-style: bold; -fx-background-color: #778899;"
            button.onAction = EventHandler {
                val node = NodeFunction().getNode(buttonTypes)
                node.layoutX += 100
                node.layoutY += 100
                root.children.add(node)
            }
            vbox.children.add(button)
        }


        for (button in btnTypes) {
            BtnCreate(button)
        }

        return vbox
    }

    private fun saveAndOpenNodesButtons(x: Double, y: Double): HBox {
        val hBox = HBox(10.0)
        hBox.style = "-fx-padding: 20px 10px;-fx-background-color: #5F9EA0;" +
                "-fx-background-radius: 15px; -fx-border-radius: 15px;"
        hBox.layoutX = x
        hBox.layoutY = y

        val buttonSave = Button(Titles.SAVE_NODES)
        buttonSave.style = "-fx-padding: 5px 10px; -fx-margin: 10px;" +
                " -fx-text-style: bold; -fx-background-color: #778899;"

        buttonSave.onAction = EventHandler {
            val gson = Gson()
            val nodes = root.children.filterIsInstance<DraggableNode>()
            val listNodes = MutableList(nodes.size) { nodes[it].toData() }
            val links = root.children.filterIsInstance<NodeLink>()
            val listLinks = MutableList(links.size) { links[it].toData() }

            println(gson.toJson(Saved(listNodes, listLinks)))

            val fileChooser = FileChooser()
            fileChooser.title = Titles.SAVE_NODES
            fileChooser.extensionFilters.addAll(
                FileChooser.ExtensionFilter(Titles.NODE_FILES, Formats.NS)
            )

            val dir = fileChooser.showSaveDialog(scene.window)
            if (dir != null) {
                try {
                    val file = File(dir.toURI())
                    file.writeText(gson.toJson(Saved(listNodes, listLinks)))
                } catch (e: IOException) {
                    println(e)
                }
            }
        }
        hBox.children.add(buttonSave)

        val buttonOpen = Button(Titles.OPEN_NODES)
        buttonOpen.style = "-fx-padding: 5px 10px; -fx-margin: 10px;" +
                " -fx-text-style: bold; -fx-background-color: #778899;"

        buttonOpen.onAction = EventHandler {
            val fileChooser = FileChooser()
            fileChooser.title = Titles.OPEN_NODES
            fileChooser.extensionFilters.addAll(
                FileChooser.ExtensionFilter(Titles.NODE_FILES, Formats.NS)
            )

            val dir = fileChooser.showOpenDialog(scene.window)
            if (dir != null) {
                try {
                    val file = File(dir.toURI())
                    if (!file.exists()) return@EventHandler

                    val data = Gson().fromJson(file.readText(), Saved::class.java)
                    if (data.links == null || data.nodes == null) return@EventHandler

                    root.children.removeIf { it is DraggableNode || it is NodeLink }

                    data.nodes.forEach {
                        val node = it.type?.let { it1 -> NodeFunction().getNode(it1) }
                        node?.fromData(it)
                        root.children.add(node)
                    }

                    data.links.forEach {

                        val inNode = root.lookup("#${it.inputNode}") as DraggableNode
                        val outNode = root.lookup("#${it.outputNode}") as DraggableNode
                        val inAnchor = root.lookup("#${it.inputAnchor}") as AnchorPane
                        val outAnchor = root.lookup("#${it.outputAnchor}") as AnchorPane

                        inAnchor.layoutX = it.inputAnchorSize.first
                        inAnchor.layoutY = it.inputAnchorSize.second

                        outAnchor.layoutX = it.outputAnchorSize.first
                        outAnchor.layoutY = it.outputAnchorSize.second

                        inNode.linkNodes(outNode, inNode, outAnchor, inAnchor, it.inputAnchor!!).id = it.id
                    }

                } catch (e: IOException) {
                    println(e)
                }
            }
        }
        hBox.children.add(buttonOpen)

        return hBox
    }

    override fun start(primaryStage: Stage) {
        nu.pattern.OpenCV.loadLocally()
        root.children.add(BtnAdd())
        root.children.add(saveAndOpenNodesButtons(150.0, 25.0))
        val end = EndNode()
        end.layoutX = width - end.rootPane!!.prefWidth - 30
        end.layoutY = height / 16
        root.children.add(end)
        primaryStage.scene = scene
        primaryStage.title = "EditorImage"
        primaryStage.show()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(mainDnD::class.java)
        }
    }
}
