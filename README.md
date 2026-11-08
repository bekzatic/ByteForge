# ByteForge - Custom PC Configurator & Store

Final project for the Software Design Patterns course. A Java Swing application where customers
configure a custom computer, get a live hardware compatibility check, undo/redo their changes,
add services, choose a pricing policy, track and cancel their order. Eleven design patterns work together.

![Configurator with the Gaming Pro preset](docs/screenshots/01_configure_gaming.png)

## Design patterns

| Pattern   | Where                                          | Author    |
|-----------|------------------------------------------------|-----------|
| Singleton | `inventory/Inventory`                          | Student 1 |
| Factory   | `factory/ComponentFactory`                     | Student 1 |
| Adapter   | `adapter/*Adapter`, `SupplierFeed`             | Student 1 |
| State     | `state/OrderStates`, `observer/Order`          | Student 1 |
| Builder   | `builder/PCBuilder`, `BuildDirector`           | Student 2 |
| Decorator | `decorator/AddOnDecorator`, `AddOns`           | Student 2 |
| Command   | `command/ConfigCommands`, `CommandHistory`     | Student 2 |
| Chain of Responsibility | `rules/CompatibilityRules`       | Student 3 |
| Strategy  | `strategy/PricingStrategy`                     | Student 3 |
| Observer  | `observer/Order`, `Listeners`                  | Student 3 |
| Facade    | `facade/StoreFacade`                           | Student 3 |

How they work together:

```mermaid
flowchart LR
  UI[Swing GUI / console demo] --> F[StoreFacade<br/>Facade]
  UI --> CMD[CommandHistory<br/>Command]
  F --> AD[Supplier adapters<br/>Adapter] --> FA[ComponentFactory<br/>Factory] --> INV[Inventory<br/>Singleton]
  F --> B[PCBuilder + Director<br/>Builder] --> R[Rule chain<br/>Chain of Responsibility]
  F --> D[Add-on services<br/>Decorator]
  F --> S[Pricing<br/>Strategy]
  F --> O[Order<br/>Observer subject] --> ST[OrderState<br/>State]
  INV -. stock events .-> L[Low-stock alert<br/>Observer]
```

## Run

Requires JDK 11+.

| Command | What it does |
|---|---|
| `run.bat` | start the GUI |
| `run.bat --console` | console demo that walks through every pattern |
| `run.bat --test` | 50 automated tests (no external libraries) |
| `run.bat --screenshots docs\screenshots` | regenerate the report screenshots |

- **IntelliJ IDEA:** open the folder and use the run configurations
  *ByteForge GUI*, *ByteForge Console demo* or *ByteForge Tests*.
- **macOS / Linux:** `javac -d out $(find src test -name "*.java") && java -cp out com.byteforge.Main`

## Things to try

- Load the *Gaming Pro* preset, then pick an Intel CPU and the RTX 4090: the rule chain lists every problem.
- Press Ctrl+Z / Ctrl+Y to undo and redo configuration changes.
- Tick services and switch the customer type to see the quote change.
- Use a card ending in `0000`: the payment is declined and the stock stays unchanged.
- In the Orders tab, cancel a PAID order (refund + parts back to stock); an ASSEMBLING one cannot be cancelled.
  Double-click an order (or press *Details...*) for its full receipt.
- In the Inventory tab, search, filter by category or low stock, and restock parts.

## Screenshots

| | |
|---|---|
| ![Incompatible build](docs/screenshots/02_incompatible_build.png) Rule chain rejects an incompatible build | ![Undo](docs/screenshots/03_undo_command.png) Three changes undone with Command |
| ![Corporate quote](docs/screenshots/04_corporate_quote.png) Corporate bulk pricing (Strategy) | ![Orders](docs/screenshots/05_orders_observer.png) Order states, cancellation and notifications |
| ![Inventory](docs/screenshots/06_inventory_singleton.png) Shared inventory, low-stock filter and restock | ![Order details](docs/screenshots/07_order_details.png) Order details: decorator line items, payment and state |

## Report

`docs/report/Report.pdf` (the HTML source is `docs/report/Report.html`): introduction, a section
per pattern with UML class diagrams and code, sequence diagrams, screenshots, test results,
conclusion and further work.
