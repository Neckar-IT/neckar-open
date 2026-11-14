/*
 * Copyright (C) 2013-2026 Neckar IT GmbH, Mössingen, Germany
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library. Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules, regardless of
 * the license terms of these independent modules, and to copy and distribute
 * the resulting combined work under terms of your choice, provided that every
 * copy of the combined work is accompanied by a complete copy of the source
 * code of this library.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package it.neckar.detekt

import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaUsualClassType

/**
 * Constants and name conventions shared by the layer-separation and serializable-wire-type rules.
 *
 * Encodes the project-wide conventions for identifying the four architectural layers:
 *
 * - **Domain**: in-memory business objects in `it.neckar.<product>.<domain>` packages without a
 *   [RestAndMongoPackageSegments] segment. Negative definition — anything that is not Entity, not
 *   DataView, not messaging, not infrastructure, not cross-layer-exempt, and lives under
 *   `it.neckar.`.
 * - **Mongo Entity**: any class implementing [MongoDocumentFqn] (or a subtype). Detected via
 *   supertype walk, not by name suffix or package — covers `MongoEntity`, `MongoUuidDocument`,
 *   `EmbeddedMongoDocument`, and any future variant.
 * - **REST/DataView**: any class annotated with [DataViewFqn] or whose simple name ends with
 *   one of [RestSuffixes].
 * - **Messaging**: any class annotated with [MessagingFqn] or living in a `messaging` package.
 *   The wire types of a transport that is not REST over HTTP — WebSocket frames, SSE messages,
 *   webhook bodies.
 *
 * Cross-layer-exempt types (value classes, enums, `*Like` marker interfaces, [CrossLayerFqn]-
 * annotated classes) may be referenced from any layer without violating the separation rules.
 *
 * Conversion between Domain and Entity/DataView happens in **top-level extension functions**
 * (`fun BookEntity.toModel(): Book`, `fun Book.toDetails(): BookDetails`, …). The exemption is
 * structural, not name-based: a top-level extension function has no enclosing `KtClassOrObject`,
 * so the layer-separation rules — which key off the host class — never fire on type references
 * inside it. Member methods named `toModel` are therefore flagged correctly; the codebase
 * convention is top-level extensions.
 *
 * All name lists are deliberately hardcoded here, not configurable via `detekt.yml`. They reflect
 * a repo-wide invariant; extending them is a central decision that requires a new release of the
 * `neckar-rules` plugin.
 */
internal object LayerConventions {

  /** Tagging interface for documents stored in MongoDB. Detection signal for "Mongo Entity". */
  const val MongoDocumentFqn: String = "it.neckar.open.mongodb.dao.MongoDocument"

  /** Annotation marking a class as a REST/view DTO. Detection signal for "DataView". */
  const val DataViewFqn: String = "it.neckar.open.annotations.DataView"

  /** Annotation marking a class as intentionally cross-layer. Exemption signal. */
  const val CrossLayerFqn: String = "it.neckar.open.annotations.CrossLayer"

  /** Kotlinx Serialization marker. Triggers the wire-type rules. */
  const val SerializableFqn: String = "kotlinx.serialization.Serializable"

  /** Raw `Instant` FQNs flagged in [SerializableMustUseNativeInstant]. Both share the same BSON-serialization problem. */
  val InstantFqns: Set<String> = setOf("kotlin.time.Instant", "kotlinx.datetime.Instant")

  /** Raw `Uuid` FQN flagged in [SerializableMustUseNativeUuid]. */
  const val uuidFqn: String = "kotlin.uuid.Uuid"

  /**
   * Class-name suffixes that mark a class as a REST/view DTO. Repo-wide convention; aligned with
   * `internal/patterns/docs/kotlin/view-projection.md` and `rest-module-layout.md`.
   */
  val RestSuffixes: Set<String> = setOf("Request", "Response", "DataView", "Details", "Preview", "TableEntry", "View")

  /**
   * Annotation marking a class as a messaging wire type. Detection signal for "Messaging".
   *
   * A name signal was considered and rejected (ADL 0179). `Payload` is a transport word, but
   * `Event` is also a business word — `MouseEvent` and `KeyEvent` in `it.neckar.events` are
   * in-memory input objects, and a domain event is a domain concept. A name-based exemption fails
   * in the wrong direction for all of them: it takes a class out of the rules without anyone
   * deciding so, while a forgotten annotation only produces a finding.
   */
  const val MessagingFqn: String = "it.neckar.open.annotations.Messaging"

  /**
   * Package segment that marks a whole protocol as messaging — the same deliberate act as the
   * annotation, made once for many classes, and the way for modules that do not want the
   * dependency. Evaluated by [isMessagingType], not by [RestAndMongoPackageSegments].
   */
  const val MessagingPackageSegment: String = "messaging"

  /**
   * Package segments whose contents are REST or Mongo wire types rather than Domain. Messaging is
   * absent on purpose: it carries a name signal too, so [isMessagingType] owns both of its halves.
   */
  val RestAndMongoPackageSegments: Set<String> = setOf("rest", "mongo")

  /**
   * Class-name endings that promise a wire layer: the REST suffixes plus the transport words a
   * message tends to carry. They do not classify anything — [WireNameRequiresMarker] uses them to
   * ask for the marker the name is already promising (ADL 0179).
   */
  val WireNameSignals: Set<String> = RestSuffixes + setOf("Payload", "Event", "Message")

  /**
   * Annotation marking a class at the boundary of a pricing or quote script: it builds the
   * `@DataView` input the script is called with, or calls the script. Infrastructure signal.
   *
   * The class lives in the domain package next to the domain classes it reads from; a package
   * segment would move it out of there, so the annotation is the only signal.
   */
  const val ScriptBoundaryFqn: String = "it.neckar.open.annotations.ScriptBoundary"

  /** Marker-interface name suffix for layer-spanning read models (e.g. `BookLike`). Exemption signal. */
  const val LikeSuffix: String = "Like"

  /** Top-level package prefix for first-party code. Domain detection excludes other packages. */
  const val NeckarPackagePrefix: String = "it.neckar."

  /**
   * Class-name suffixes for infrastructure / orchestration layers (per CLAUDE.md class-suffix
   * convention). These classes live in domain packages but are not Domain types — they wire
   * Domain together with persistence, REST, and events. The layer-separation rules do not apply
   * to them; they are allowed to reference Entity, REST/DataView, and Domain freely.
   */
  val InfrastructureSuffixes: Set<String> = setOf(
    "Support",        // business-logic orchestrator
    "Access",         // data layer
    "Client",         // HTTP client for a foreign service
    "Fetcher",        // background loader that holds a foreign system's data in memory
    "Projector",      // derives the read model a service answers from
    "Routing",        // HTTP routing
    "RoutingExt",     // routing extensions
    "Service",        // Ktor entry point
    "ServiceCommand", // CLI bootstrap / composition root (Clikt BaseCommand)
    "Endpoints",      // URL constants
    "Hub",            // event hub
    "Bootstrap",      // service-startup provisioning (wires Domain + persistence at boot)
    "Wiring",         // composition root of an entity module (e.g. BookOrderWiring)
    "Factory",        // test/example factories
    "Example",        // pattern fixtures (e.g. BookExample)
    "Generator",      // demo-data generators
    "Seed",           // initial data: a step writing it through a SeedTarget, or a source like JwksSeed
    "Seeder",         // seed step, agent-noun form (e.g. BranchCustomDataSeeder)
    "Test",           // test classes (also excluded via `excludes` config, but defense in depth)
    "Tests",          // test classes
  )
}

// Symbol-based predicates — context-free (no KaSession receiver required).

/** True when [this] implements [LayerConventions.MongoDocumentFqn] (transitively). */
internal fun KaClassSymbol.isMongoEntity(): Boolean {
  if (classKind == KaClassKind.COMPANION_OBJECT) return false
  if (classFqnString() == LayerConventions.MongoDocumentFqn) return true
  return collectAllSupertypeFqns().contains(LayerConventions.MongoDocumentFqn)
}

/**
 * True when [this] is a REST/DataView class — `@DataView` annotation, own name has a REST suffix,
 * or any enclosing class name has a REST suffix (covers sealed-leaf inner classes like
 * `CreateBookResponse.Good`).
 *
 * Companion objects are not classified as REST — they are factory holders, not wire DTOs.
 */
internal fun KaClassSymbol.isRestDataView(): Boolean = isRestDataView(considerNames = true)

/**
 * [isRestDataView] with the name signal switchable — `considerNames = false` asks whether the
 * annotation alone says it.
 */
internal fun KaClassSymbol.isRestDataView(considerNames: Boolean): Boolean {
  if (classKind == KaClassKind.COMPANION_OBJECT) return false
  if (hasAnnotationFqn(LayerConventions.DataViewFqn)) return true
  return considerNames && hasNameSignature(LayerConventions.RestSuffixes)
}

/**
 * True when [this] carries [LayerConventions.MessagingFqn] — on itself or on the enclosing parent it
 * inherits from — or lives in a [LayerConventions.MessagingPackageSegment] package.
 *
 * The enclosing walk carries a sealed hierarchy's statement to its leaves: annotating
 * `PushWebhookPayload` says it for `PushWebhookPayload.Commit` too. A whole protocol declares itself
 * by its package instead, once for all of its types.
 */
internal fun KaClassSymbol.isMessagingType(): Boolean {
  if (classKind == KaClassKind.COMPANION_OBJECT) return false
  if (hasAnnotationFqn(LayerConventions.MessagingFqn)) return true
  if (inheritsAnnotationFromEnclosingParent(LayerConventions.MessagingFqn)) return true
  val packageFqn = classId?.packageFqName?.asString() ?: return false
  if (!packageFqn.startsWith(LayerConventions.NeckarPackagePrefix)) return false
  return packageFqn.containsSegment(LayerConventions.MessagingPackageSegment)
}

/**
 * True when the simple name of [this] — or of any enclosing class — ends with one of [suffixes].
 *
 * The enclosing walk carries a class's signature to every type nested in it, so that
 * `CreateBookResponse.Good` counts as REST and `TenantSupport.StoreResult.Stored` as infrastructure.
 */
private fun KaClassSymbol.hasNameSignature(suffixes: Set<String>): Boolean {
  var currentClassId = classId ?: return false
  while (true) {
    val name = currentClassId.shortClassName.asString()
    if (suffixes.any { name.endsWith(it) }) return true
    currentClassId = currentClassId.outerClassId ?: return false
  }
}

/**
 * True when [this] is a Domain class — under `it.neckar.`, package carries no REST or Mongo segment
 * ([LayerConventions.RestAndMongoPackageSegments]), not Entity, not DataView, not a messaging type,
 * not cross-layer-exempt, not an infrastructure class (Support/Access/Routing/…, `@ScriptBoundary`)
 * and not nested in one.
 * Includes regular classes and sealed-hierarchy interfaces (e.g. `sealed interface Book :
 * DomainObject` is a Domain type, not a marker).
 *
 * Package-segment exclusion catches sealed-leaf classes that live in `*.rest.messages` / `*.mongo`
 * but lack a name signature of their own (e.g. `CreateBookResponse.Good`); the `messaging` package
 * does the same through [isMessagingType].
 */
internal fun KaClassSymbol.isDomainClass(): Boolean = isDomainClass(considerRestNames = true)

/**
 * [isDomainClass] with the REST name signal switchable.
 *
 * `considerRestNames = false` answers the question [WireNameRequiresMarker] asks: would this class
 * be Domain if its name did not speak for it? A `*Details` class without `@DataView` says yes — and
 * that is the gap the name papers over.
 */
internal fun KaClassSymbol.isDomainClass(considerRestNames: Boolean): Boolean {
  if (classKind == KaClassKind.COMPANION_OBJECT) return false
  val packageFqn = classId?.packageFqName?.asString() ?: return false
  if (!packageFqn.startsWith(LayerConventions.NeckarPackagePrefix)) return false
  if (LayerConventions.RestAndMongoPackageSegments.any { packageFqn.containsSegment(it) }) return false
  if (isCrossLayerExempt()) return false
  if (isMongoEntity()) return false
  if (isRestDataView(considerNames = considerRestNames)) return false
  if (isMessagingType()) return false
  if (isInfrastructureClass()) return false
  return classKind == KaClassKind.CLASS || classKind == KaClassKind.INTERFACE
}

/** True when the simple name of [this] — or of an enclosing class — promises a wire layer. */
internal fun KaClassSymbol.hasWireNameSignal(): Boolean = hasNameSignature(LayerConventions.WireNameSignals)

/** True when the dot-separated [this] FQN contains [segment] as a whole segment (not as substring). */
private fun String.containsSegment(segment: String): Boolean = ".$this.".contains(".$segment.")

/**
 * True when the class carries [LayerConventions.ScriptBoundaryFqn] or its name or an enclosing class
 * name ends with one of [LayerConventions.InfrastructureSuffixes]: `TenantSupport.StoreResult.Stored`
 * belongs to `TenantSupport`.
 */
internal fun KaClassSymbol.isInfrastructureClass(): Boolean {
  if (hasAnnotationFqn(LayerConventions.ScriptBoundaryFqn)) return true
  return hasNameSignature(LayerConventions.InfrastructureSuffixes)
}

/**
 * True when [this] is exempt from layer-separation rules: value class, enum, `*Like` marker
 * interface, or [LayerConventions.CrossLayerFqn]-annotated class.
 */
internal fun KaClassSymbol.isCrossLayerExempt(): Boolean {
  if (hasAnnotationFqn(LayerConventions.CrossLayerFqn)) return true
  if (inheritsCrossLayerFromEnclosingParent()) return true
  if (classKind == KaClassKind.ENUM_CLASS) return true
  if (isValueClass()) return true
  val name = simpleName()
  if (name != null && name.endsWith(LayerConventions.LikeSuffix) && classKind == KaClassKind.INTERFACE) return true
  return false
}

/**
 * True when a `@CrossLayer` type both **encloses** [this] and is one of its **supertypes** — the
 * shape of a sealed hierarchy that declares itself once: `Url.Absolute` is cross-layer because
 * `Url` is, and repeating the annotation on every leaf would only restate the parent.
 *
 * Both conditions are required. A supertype walk alone would over-exempt: `DomainObjectRepresentation`
 * is `@CrossLayer` and implemented across the repository, so inheriting the exemption from any
 * supertype would silence the layer rules for every one of its implementors.
 *
 * Only **direct** supertypes count, unlike the transitive walk in [isMongoEntity]. A leaf declares
 * the parent it belongs to; inheriting across a chain would widen the exemption again.
 */
private fun KaClassSymbol.inheritsCrossLayerFromEnclosingParent(): Boolean =
  inheritsAnnotationFromEnclosingParent(LayerConventions.CrossLayerFqn)

/**
 * True when a type annotated [annotationFqn] both **encloses** [this] and is one of its **direct
 * supertypes** — the shape of a sealed hierarchy that declares itself once.
 *
 * Both conditions are required, and the supertype must be direct. A supertype walk alone would
 * over-exempt every implementor of a widely implemented annotated interface; a transitive walk would
 * widen the exemption along the chain.
 */
private fun KaClassSymbol.inheritsAnnotationFromEnclosingParent(annotationFqn: String): Boolean {
  val enclosingClassIds = generateSequence(classId?.outerClassId) { it.outerClassId }.toSet()
  if (enclosingClassIds.isEmpty()) return false
  return superTypes.any { superType ->
    val classType = superType as? KaClassType ?: return@any false
    if (classType.classId !in enclosingClassIds) return@any false
    (classType.symbol as? KaClassSymbol)?.hasAnnotationFqn(annotationFqn) == true
  }
}

/** True when [this] carries `@kotlinx.serialization.Serializable`. */
internal fun KaClassSymbol.isSerializableClass(): Boolean = hasAnnotationFqn(LayerConventions.SerializableFqn)

// Type-based predicates for the wire-type rules.

/**
 * True when [type] is one of [LayerConventions.InstantFqns] **as written**.
 *
 * Uses the abbreviated type when available so that typealiases are treated as their alias name —
 * `NativeInstant` (typealias for `Instant` with a custom serializer) is **not** flagged even
 * though it expands to `kotlin.time.Instant` at the type level.
 */
internal fun typeIsInstant(type: KaType): Boolean {
  return type.declaredFqn() in LayerConventions.InstantFqns
}

/**
 * True when [type] is [LayerConventions.uuidFqn] **as written**.
 *
 * Uses the abbreviated type so `NativeUuid` (typealias for `Uuid`) is **not** flagged.
 */
internal fun typeIsUuid(type: KaType): Boolean {
  return type.declaredFqn() == LayerConventions.uuidFqn
}

/**
 * Returns the FQN of the type **as it was written in source**. If the type was declared via a
 * typealias, returns the typealias's FQN (preserving e.g. `NativeInstant` over `kotlin.time.Instant`).
 */
private fun KaType.declaredFqn(): String? {
  (this as? KaUsualClassType)?.abbreviation?.let { return it.classId.asFqNameString() }
  val classType = this as? KaClassType ?: return null
  return classType.classId.asFqNameString()
}

// Internal helpers.

private fun KaClassSymbol.classFqnString(): String? = classId?.asFqNameString()

private fun KaClassSymbol.simpleName(): String? = (this as? KaNamedClassSymbol)?.name?.asString()

private fun KaClassSymbol.hasAnnotationFqn(annotationFqn: String): Boolean {
  return annotations.any { it.classId?.asFqNameString() == annotationFqn }
}

internal fun KaClassSymbol.isValueClass(): Boolean {
  val named = this as? KaNamedClassSymbol ?: return false
  return named.isInline
}

private fun KaClassSymbol.collectAllSupertypeFqns(): Set<String> {
  val result = mutableSetOf<String>()
  fun collect(symbol: KaClassSymbol) {
    symbol.superTypes.forEach { superType ->
      val classType = superType as? KaClassType ?: return@forEach
      val fqn = classType.classId.asFqNameString()
      if (fqn !in result) {
        result += fqn
        (classType.symbol as? KaClassSymbol)?.let(::collect)
      }
    }
  }
  collect(this)
  return result
}
