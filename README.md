# ByteForge - Custom PC Configurator & Store

Final project for the Software Design Patterns course. A Java Swing application where customers
configure a custom computer, get a live hardware compatibility check, undo/redo their changes,
add services, choose a pricing policy, track and cancel their order. Ten design patterns work together.

| Pattern   | Where                                          | Author    |
|-----------|------------------------------------------------|-----------|
| Singleton | `inventory/Inventory`                          | Student 1 |
| Factory   | `factory/ComponentFactory`                     | Student 1 |
| Adapter   | `adapter/*Adapter`, `SupplierFeed`             | Student 1 |
| State     | `state/OrderStates`, `observer/Order`          | Student 1 |
| Builder   | `builder/PCBuilder`, `BuildDirector`           | Student 2 |
| Decorator | `decorator/AddOnDecorator`, `AddOns`           | Student 2 |
| Command   | `command/ConfigCommands`, `CommandHistory`     | Student 2 |
| Strategy  | `strategy/PricingStrategy`                     | Student 3 |
| Observer  | `observer/Order`, `Listeners`                  | Student 3 |
| Facade    | `facade/StoreFacade`                           | Student 3 |

## Run

Requires JDK 11+.

- `run.bat` - start the GUI
- `run.bat --console` - console demo of all patterns
- `run.bat --test` - 38 automated tests (no external libraries)
- `run.bat --screenshots docs\screenshots` - regenerate report screenshots
- IntelliJ IDEA: open the folder and use the run configurations
  *ByteForge GUI*, *ByteForge Console demo* or *ByteForge Tests*.

Defense tips: Ctrl+Z / Ctrl+Y undo and redo configuration changes; a card ending in `0000`
is declined and the stock stays unchanged; a PAID order can be cancelled, an ASSEMBLING one cannot.

Report: `docs/report/Report.pdf`. The HTML source is `docs/report/Report.html`.
