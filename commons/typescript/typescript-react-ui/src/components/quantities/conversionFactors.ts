/**
 * Unit conversion factors
 *
 * The REST API will use the base units to transmit measurement values.
 *
 * TODO these factors also stand as `toBaseUnit:` in the unit specs of the spec projects and could
 * be derived from them — blocked on this file living in an open module while the generator is closed.
 */
export const conversionFactors = {
  /**
   * Current (Base unit: Ampere [A])
   */
  current: {
    toAmpere: 1,
  },

  /**
   * Energy (Base unit: kilowatt-hour [kWh])
   */
  energy: {
    toKiloWattHours: 1,
  },

  /**
   * Fraction (Base unit: fraction — 0.19 is 19 %)
   */
  fraction: {
    toPercent: 100,
  },

  /**
   * Length (Base unit: meters [m])
   */
  length: {
    toMillimeters: 1000,
    toCentimeters: 100,
    toMeters: 1,
    toKilometers: 0.001,
  },

  /**
   * Currency (Base unit: EuroCent [Ct])
   */
  money: {
    toCent: 1,
    toEuro: 0.01,
  },

  /**
   * Power (Base unit: Watt [W])
   */
  power: {
    toWatt: 1,
    toKiloWatt: 0.001,
  },

  /**
   * Time (Base unit: hours [h])
   */
  time: {
    toHours: 1,
  },

  /**
   * Voltage (Base unit: Volt [V])
   */
  voltage: {
    toVolt: 1,
  },
};
