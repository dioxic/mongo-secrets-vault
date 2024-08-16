package uk.dioxic.mongo.secrets;

public enum Color {
    BLUE,
    GREEN;

    public Color flip() {
        switch (this){
            case BLUE -> {
                return GREEN;
            }
            case GREEN -> {
                return BLUE;
            }
            default -> throw new IllegalStateException("Cannot flip " + this);
        }
    }

}
