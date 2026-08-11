package CustomerData;

import enums.Kind;

public record Event(double time, Kind kind, CustomerRecord customer) {

}
