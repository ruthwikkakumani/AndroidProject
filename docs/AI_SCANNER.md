# FindIt photo assistant

FindIt runs two genuine, bundled Google ML Kit models on the Android device:

| Feature | Android library | Behavior |
| --- | --- | --- |
| Image labeling | `com.google.mlkit:image-labeling` | Suggests broad object/category labels with model confidence. It analyzes the whole input image; it is not a custom campus-item detector. |
| Text recognition | `com.google.mlkit:text-recognition` (Latin recognizer) | Reads visible writing from a photograph. The student can edit the result. |

Neither feature calls an OpenAI, Gemini, or other paid cloud AI API. No AI key is required. Models are included with the app, so inference works offline. Firebase configuration serves authentication, shared reports, photos and chat; the Maps key serves map tiles. Those services are separate from AI inference.

## Why the emulator returned room / tile / shelf

The emulator camera was photographing its virtual room. The decorative pouch in the previous scanner header was not the submitted image. Recognizing the room did not mean the model had identified a lost item.

The revised scanner shows the actual submitted photo. Known scene labels are separated from useful item suggestions and are not used as label evidence in chat photo comparison. No object label is automatically inserted into the search field: students explicitly choose a clue. OCR writing populates an editable field. Queries search the campus reports using the repository's text matching; this search is not an LLM.

Percentages describe the model's confidence in a category label. They are not an exact-item identification score, recovery rate, or ownership probability. The model can miss objects or suggest the wrong category, especially with clutter, small items or poor lighting. A close-up improves the input; the app does not invent an item when it sees only surroundings.

## Demonstrating it in college

1. Use **Choose photo** with a clear actual item photo on the emulator, or use a physical phone's camera. Its virtual-room camera is not a real camera feed of the presenter.
2. For OCR, photograph a readable notebook label or a large printed item name in good light.
3. Review the displayed photo, choose an item clue or edit the extracted text, then review matching campus reports.
4. Open **How the scanner works** for the explanation sheet.

## Chat photo evidence

The photo checker combines ML Kit labels, shared OCR words, average-hash structure and average-color similarity. These heuristics provide supporting evidence; they are not a trained exact-item verification model. Different backgrounds and viewpoints affect the comparison. Private questions and both students' confirmation remain required before handing over an item.
