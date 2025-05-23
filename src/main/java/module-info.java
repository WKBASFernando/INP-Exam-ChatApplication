module com.ijse.inpexamprojectchatapplication {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;


    opens com.ijse.inpexamprojectchatapplication to javafx.fxml;
    exports com.ijse.inpexamprojectchatapplication;
    exports com.ijse.inpexamprojectchatapplication.Client;
    opens com.ijse.inpexamprojectchatapplication.Client to javafx.fxml;
    exports com.ijse.inpexamprojectchatapplication.Server;
    opens com.ijse.inpexamprojectchatapplication.Server to javafx.fxml;
}