# Daftar (دفتر)

A native Android app for keeping the books of several small businesses on one phone.
Kotlin, Jetpack Compose, Material 3. English and Arabic (right-to-left), light and dark.
All data stays on the phone.

## What it does

| Area | What you can do |
| --- | --- |
| Businesses | Keep any number of businesses and switch between them from the home screen. Each has its own TRN, currency (AED by default), VAT rate (5% by default), invoice numbering and colour. |
| Dashboard | Net profit, income and expenses for a month, quarter, year or all time; a six-month chart; VAT to pay; unpaid invoices; loans outstanding; capital invested. |
| Invoices | Create tax invoices with line items, mixed VAT rates and a discount. Share as an A4 PDF (mirrored layout in Arabic). Mark paid; overdue ones are flagged. |
| Quotations | Same editor, with a "valid until" date. Turn an accepted quotation into an invoice in one tap. |
| Purchases | Import a supplier invoice from the camera or a picture and have the supplier, TRN, invoice number, date, subtotal, VAT and total filled in for checking. Or type it in. |
| Income and expenses | Quick entries for money with no invoice behind it (rent, salaries, cash sales), with VAT split out automatically. |
| Products | Name, code, unit, selling price, cost, VAT rate, stock. Pick them when writing an invoice. |
| Shareholders | Who invested how much, with ownership worked out from the amounts or set by hand. |
| Loans | Lender, amount, interest, due date, and each repayment; shows what is still owed. |
| Tax | VAT summary per period (charged on sales minus paid on purchases), a VAT calculator, and a rough UAE corporate tax estimate. |
| Settings | Language, appearance, and backup/restore to a single file. |

## Getting an installable app (APK)

This project was written without access to Google's Android libraries, so **it has not been
compiled yet**. The first build may show a few compiler errors; send the error text back and
they can be fixed quickly.

### Option A: let GitHub build it (nothing to install)

1. Create a free account at github.com and a new **private** repository.
2. Upload the contents of this folder to it (drag and drop in the browser works). Include the
   hidden `.github` folder, which holds the build instructions. On a Mac, press
   Cmd + Shift + . in Finder to show hidden folders.
3. Open the repository's **Actions** tab. A run called "Build APK" starts by itself.
4. When it turns green, open the run and download **Daftar-debug-apk** at the bottom.
5. Unzip it, copy `app-debug.apk` to your phone and open it. Android will ask you to allow
   installing from that source.

If the run turns red, open it, copy the red error lines, and send them back.

### Option B: Android Studio

1. Install Android Studio (Ladybug or newer).
2. **File > Open** and choose this folder. Wait for the first sync to finish.
3. Plug in your phone with USB debugging on (or start an emulator) and press **Run**.

## How reading an invoice photo works

Text is recognised on the phone with Google ML Kit; the picture is never uploaded.
The app then looks for a 15-digit TRN, an invoice number, a date, and the subtotal, VAT and
total lines. Item lines are only filled in when the arithmetic on the row proves them
(quantity x unit price = amount) and they add up to the subtotal; otherwise only the totals
are filled in. You always get a screen to check before anything is saved.

Limits to know about:

- ML Kit's on-device reader handles Latin script. Bilingual UAE invoices work because the
  figures and English labels are read; invoices written **only in Arabic** need typing in.
- If VAT is not printed clearly it is worked out from the total at the business's VAT rate,
  and the app says so.
- Crumpled, dark or angled photos read badly. Flat and well lit is best.

## How the numbers are counted

- Income = sales invoices + income entries. Expenses = purchase invoices + expense entries.
  Both exclude VAT. Quotations never count. Loan repayments and shareholder capital are not
  income or expenses.
- An invoice counts from its date, whether or not it has been paid.
- VAT periods follow calendar months and quarters. Your FTA tax period may be offset.
- The corporate tax figure is 9% of this calendar year's profit above AED 375,000. It is a
  guide, not a tax computation.
- Product stock is a number you maintain; invoices do not change it automatically.

This app is a record-keeping tool, not tax advice.

## What has been checked

- The tax maths and the invoice-text reader are plain Kotlin and were compiled and run:
  `app/src/test/java/com/daftar/app/DomainTest.kt` (9 tests, all passing).
- Every Kotlin file was type-checked with the Kotlin 2.0.21 compiler against hand-written
  stand-ins for the Android libraries, with no errors. That catches typos, missing imports and
  type mistakes, but not a mismatch with the real library versions.
- Every text key used in code exists in both the English and Arabic files.
- Not done: a real build, and running the app on a phone. Layouts have not been seen on a
  screen yet.

## Project layout

```
app/src/main/java/com/daftar/app/
  MainActivity.kt
  domain/   Money.kt (VAT, totals, periods)   InvoiceParser.kt (photo text -> fields)
  data/     Entities.kt   AppDao.kt (Room database)   Prefs.kt
  io/       OcrScanner.kt (ML Kit)   PdfExporter.kt (A4 invoices)
  ui/       Theme.kt  Common.kt  Nav.kt  AppViewModel.kt
            HomeScreen.kt  DocsScreens.kt  MoneyScreens.kt  ProductScreens.kt
            PeopleScreens.kt (shareholders, loans)  TaxScreen.kt  SettingsScreens.kt
app/src/main/res/values/strings.xml       English
app/src/main/res/values-ar/strings.xml    Arabic
```

Colours and type live in `ui/Theme.kt`. To rename the app, change `app_name` in both
`strings.xml` files.
