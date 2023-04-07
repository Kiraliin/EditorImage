module com.kiralin.editorimage {
    requires javafx.controls;
    requires javafx.fxml;
    requires kotlin.stdlib;
    requires com.google.gson;
    requires opencv;


    opens com.kiralin.editorimage to javafx.fxml;
    exports com.kiralin.editorimage;
}