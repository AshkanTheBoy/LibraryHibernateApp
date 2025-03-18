//custom Exception class for the wrong user input
class WrongInputException extends Exception{
    String message;

    public WrongInputException(String input){
        message = "Input: "+input;
    }

    public String getMessage(){
        return message;
    }
    public String toString(){
        return message;
    }
}
