/**
 * A recreation of the Spring PetClinic Sample Application, using Java EE 7 for UI controllers and business classes.
 * JMockit is used for integration testing, instead of the Spring Test Context.
 * <p/>
 * Given the small size and simplicity of the application, it has minimal package structure.
 * A larger real-world codebase should separate classes also in layers, rather than just in functional areas.
 * <p/>
 * Using the architectural layers of DDD (Domain Driven Design), we could apply one of the two following structures:
 * <pre>
 * petclinic
 *    owners
 *       application       (OwnerScreen)
 *       domain            (Owner, OwnerMaintenance)
 *    pets
 *       application       (PetScreen)
 *       domain            (Pet, PetType, PetMaintenance)
 *    vets
 *       application       (VetScreen)
 *       domain            (Vet, Specialty, VetMaintenance)
 *    visits
 *       application       (VisitScreen)
 *       domain            (Visit, VisitMaintenance)
 *    infrastructure
 *       persistence       (Database)
 *       domain            (BaseEntity, Person)
 * </pre>
 * or
 * <pre>
 * petclinic
 *    application
 *       owners            (OwnerScreen)
 *       pets              (PetScreen)
 *       vets              (VetScreen)
 *       visits            (VisitScreen)
 *    domain
 *       owners            (Owner, OwnerMaintenance)
 *       pets              (Pet, PetType, PetMaintenance)
 *       vets              (Vet, Specialty, VetMaintenance)
 *       visits            (Visit, VisitMaintenance)
 *    infrastructure
 *       persistence       (Database)
 *       domain            (BaseEntity, Person)
 * </pre>
 * Either of the above is good, from the point of view of having cohesive packages.
 * The package structure used in Spring's PetClinic is not a good one, because it groups together classes that are
 * unrelated to each other, therefore violating the basic principle that packages should have high cohesion.
 * For example, take the <tt>petclinic.web</tt> package: it holds both <tt>OwnerController</tt> and
 * <tt>VetController</tt>, which are completely unrelated and independent of each other.
 */
package petclinic;