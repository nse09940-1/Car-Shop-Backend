package main.domain;

public record Money(long rubles) {

    public Money add(Money other) {
        return new Money(this.rubles + other.rubles);
    }

    public Money subtract(Money other) {
        return new Money(this.rubles - other.rubles);
    }
}


